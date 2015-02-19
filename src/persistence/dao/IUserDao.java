package persistence.dao;

import model.User;

public interface IUserDao
{
   public User findByUserName(final String username);

   public boolean store(final User user);

}
