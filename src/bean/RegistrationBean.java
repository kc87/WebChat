package bean;


import model.User;
import persistence.dao.HibernateImpl;
import persistence.dao.IUserDao;
import util.PasswordStore;

import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;

@Named
@RequestScoped
public class RegistrationBean implements Serializable
{
   @Inject
   private User user;
   private IUserDao userDao = new HibernateImpl();
   private String password;

   public User getUser()
   {
      return user;
   }

   public void setUser(User user)
   {
      this.user = user;
   }

   public String getPassword()
   {
      return password;
   }

   public void setPassword(String password)
   {
      this.password = password;
   }

   public String doRegister()
   {
      try {
         String encryptedPassword = PasswordStore.encryptPassword(password);
         user.setPwHash(encryptedPassword);

         if (userDao.store(this.user)) {
            return "chat?faces-redirect=true";
         }

      } catch (RuntimeException e) {
         addFacesMessage(e.getMessage());
      }

      return null;
   }


   private void addFacesMessage(final String msg)
   {
      FacesMessage facesMsg = new FacesMessage(msg);
      facesMsg.setSeverity(FacesMessage.SEVERITY_ERROR);
      FacesContext.getCurrentInstance().addMessage(null, facesMsg);
   }
}
