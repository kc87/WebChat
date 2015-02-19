package util;


import org.pmw.tinylog.Logger;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class PasswordStore
{
   private static final String HASH_ALGO = "SHA-256";
   private static final int SALT_SIZE = 4;

   private PasswordStore()
   {
   }

   public static boolean isPasswordCorrect(final String password, final String hash)
   {
      Logger.debug("PW:" + password + " hash:" + hash);
      String salt = hash.split(":")[1];
      return hashString(password + salt).equals(hash.split(":")[0]);
   }


   public static String encryptPassword(final String password)
   {
      String result;
      Random rnd = new Random();
      StringBuilder saltStringBuilder = new StringBuilder();

      //generate SALT_SIZE byte random salt:
      for (int i = 0; i < SALT_SIZE; i++) {
         saltStringBuilder.append(Integer.toString((rnd.nextInt(255) & 0xff), 16));
      }

      //TODO: get rid of the colon and use SALT_SIZE instead
      result = hashString(password + saltStringBuilder.toString()) + ":" + saltStringBuilder.toString();

      if (result == null) {
         throw new RuntimeException("500: Internal Server Error!");
      }

      return result;
   }


   private static String hashString(final String str)
   {
      MessageDigest md;

      try {
         md = MessageDigest.getInstance(HASH_ALGO);
      } catch (NoSuchAlgorithmException e) {
         Logger.error(e);
         throw new RuntimeException(e);
      }

      md.update(str.getBytes());
      byte[] dataBytes = md.digest();

      StringBuffer sb = new StringBuffer();
      for (int i = 0; i < dataBytes.length; i++) {
         sb.append(Integer.toString((dataBytes[i] & 0xff), 16));
      }

      return sb.toString();
   }
}
