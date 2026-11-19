package com.lifeorganizer;
import java.security.*; import java.sql.*; import java.util.Base64;
import javax.crypto.SecretKeyFactory; import javax.crypto.spec.PBEKeySpec;
public final class Auth {
 private Auth(){}
 private static String hash(char[] password)throws Exception{
  byte[] salt=new byte[16]; SecureRandom.getInstanceStrong().nextBytes(salt);
  PBEKeySpec spec=new PBEKeySpec(password,salt,210000,256);
  byte[] key=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); spec.clearPassword();
  return "pbkdf2$210000$"+Base64.getEncoder().encodeToString(salt)+"$"+Base64.getEncoder().encodeToString(key);
 }
 private static boolean verify(char[] password,String stored)throws Exception{
  String[] p=stored.split("\\$"); if(p.length!=4||!p[0].equals("pbkdf2"))return false;
  byte[] salt=Base64.getDecoder().decode(p[2]); PBEKeySpec spec=new PBEKeySpec(password,salt,Integer.parseInt(p[1]),256);
  byte[] key=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); spec.clearPassword();
  return MessageDigest.isEqual(key,Base64.getDecoder().decode(p[3]));
 }
 public static int register(String name,String email,char[] password)throws Exception{
  if(name.isBlank()||!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")||password.length<8)
   throw new IllegalArgumentException("Enter a name, valid email, and password of at least 8 characters.");
  try(Connection c=Db.connect();PreparedStatement ps=c.prepareStatement(
   "INSERT INTO users(name,email,password_hash) VALUES(?,?,?)",Statement.RETURN_GENERATED_KEYS)){
   ps.setString(1,name.trim());ps.setString(2,email.trim().toLowerCase());ps.setString(3,hash(password));ps.executeUpdate();
   try(ResultSet rs=ps.getGeneratedKeys()){rs.next();return rs.getInt(1);}
  }
 }
 public static int login(String email,char[] password)throws Exception{
  try(Connection c=Db.connect();PreparedStatement ps=c.prepareStatement("SELECT user_id,password_hash FROM users WHERE email=?")){
   ps.setString(1,email.trim().toLowerCase());try(ResultSet rs=ps.executeQuery()){
    if(!rs.next()||!verify(password,rs.getString(2)))return -1;return rs.getInt(1);
   }
  }
 }
}
