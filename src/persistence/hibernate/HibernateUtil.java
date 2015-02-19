package persistence.hibernate;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.pmw.tinylog.Logger;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;


public class HibernateUtil
{

   private static SessionFactory sessionFactory;

   static {
      Configuration configuration = new Configuration().configure("/META-INF/hibernate.cfg.xml");
      StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder().applySettings(configuration.getProperties());
      sessionFactory = configuration.buildSessionFactory(builder.build());
   }


   public static SessionFactory getSessionFactory()
   {
      return sessionFactory;
   }

   // Gracefully shut down db engine, so the container doesn't complain!
   public static void shutdown()
   {
      sessionFactory.close();

      Enumeration<Driver> drivers = DriverManager.getDrivers();
      while (drivers.hasMoreElements()) {
         Driver driver = drivers.nextElement();
         try {
            DriverManager.deregisterDriver(driver);
            Logger.debug("Unregister jdbc driver: {0}", driver);
         } catch (SQLException e) {
            Logger.error("Error unregister driver {0}", driver);
         }
      }
   }
}