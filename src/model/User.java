package model;

import org.hibernate.validator.constraints.NotEmpty;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

@Entity
@Table(name = "user_tbl")
public class User implements Serializable
{
   @Id
   @GeneratedValue(strategy = GenerationType.AUTO)
   @Column(name = "ID")
   private Long id;

   @Column(name = "FIRSTNAME")
   @NotEmpty
   private String firstName;

   @Column(name = "LASTNAME")
   @NotEmpty
   private String lastName;

   @Column(name = "EMAIL")
   @Pattern(regexp = "\\w+[.-]*\\w+@\\w+\\.\\w+", message = "Not a valid email address!")
   private String email;

   @Column(name = "USERNAME")
   @Size(min = 2, max = 64)
   private String username;

   @Column(name = "PWHASH")
   private String pwHash;

   public Long getId()
   {
      return id;
   }

   public void setId(Long id)
   {
      this.id = id;
   }

   public String getFirstName()
   {
      return firstName;
   }

   public void setFirstName(String firstName)
   {
      this.firstName = firstName;
   }

   public String getLastName()
   {
      return lastName;
   }

   public void setLastName(String lastName)
   {
      this.lastName = lastName;
   }

   public String getEmail()
   {
      return email;
   }

   public void setEmail(String email)
   {
      this.email = email;
   }

   public String getUsername()
   {
      return username;
   }

   public void setUsername(String username)
   {
      this.username = username;
   }

   public String getPwHash()
   {
      return pwHash;
   }

   public void setPwHash(String pwHash)
   {
      this.pwHash = pwHash;
   }
}
