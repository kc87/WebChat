package persistence.dao;

import model.User;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.pmw.tinylog.Logger;
import persistence.hibernate.HibernateUtil;

import java.util.List;

public class HibernateImpl implements IUserDao
{
   @Override
   @SuppressWarnings("unchecked")
   public User findByUserName(String username)
   {
      List<User> userList;
      Session session = HibernateUtil.getSessionFactory().openSession();
      session.beginTransaction();
      Criteria criteria = session.createCriteria(User.class);

      //Case-insensitive !!!
      criteria.add(Restrictions.ilike("username", username, MatchMode.ANYWHERE));

      userList = criteria.list();

      session.getTransaction().commit();
      session.close();

      return (userList != null && userList.size() > 0) ? userList.get(0) : new User();
   }

   @Override
   public boolean store(User user)
   {
      if (isUserNameInUse(user.getUsername())) {
         throw new RuntimeException("Username \"" + user.getUsername() + "\" is already taken!");
      }

      Session session = HibernateUtil.getSessionFactory().openSession();
      Transaction tx = null;

      try {
         tx = session.beginTransaction();
         session.saveOrUpdate(user);
         tx.commit();
         return tx.wasCommitted();
      } catch (HibernateException e) {
         Logger.error(e);
         if (tx != null) {
            tx.rollback();
         }
         return false;
      } finally {
         session.close();
      }
   }

   private boolean isUserNameInUse(final String username)
   {
      return findByUserName(username).getId() != null;
   }
}
