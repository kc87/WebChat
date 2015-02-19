package persistence.jpa;


import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class JpaUtil
{
   private static final EntityManagerFactory entityManagerFactory;

   static {
      try {
         entityManagerFactory = Persistence.createEntityManagerFactory("hibernate.sqlite.persistence");
         System.out.println("Entity Manager Test:" + entityManagerFactory);
      } catch (Throwable ex) {

         System.err.println("Initial SessionFactory creation failed." + ex);
         throw new ExceptionInInitializerError(ex);
      }
   }

   public static EntityManagerFactory getEntityManagerFactory()
   {
      return entityManagerFactory;
   }

}
