package ws;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import model.User;
import org.pmw.tinylog.Logger;
import persistence.dao.HibernateImpl;
import persistence.dao.IUserDao;
import persistence.hibernate.HibernateUtil;
import util.PasswordStore;
import ws.protocol.ChatMsg;
import ws.protocol.LoginMsg;
import ws.protocol.Message;
import ws.protocol.ResultMsg;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@ServerEndpoint("/ws")
public class WsServer implements ServletContextListener
{
   private static final int IDLE_TIMEOUT_SEC = 60;
   private static final String[] PEER_COLORS = {"#38F", "#f00", "#ff0", "#f08", "#0ff", "#888", "#8ff", "#f6f", "#ff4", "#fff"};
   private static final int PEER_COLOR_NB = PEER_COLORS.length;
   private static final AbstractMap<String, String> userColorMap = new ConcurrentHashMap<>();
   private static AtomicInteger usersLoggedIn = new AtomicInteger(0);
   private final IUserDao userDao = new HibernateImpl();
   private Session thisSession = null;

   @OnOpen
   public void onOpen(Session session)
   {
      Logger.debug("onOpen(): " + session.getId());
      thisSession = session;
      thisSession.setMaxIdleTimeout(IDLE_TIMEOUT_SEC * 1000);
      thisSession.getUserProperties().clear();
   }

   @OnMessage
   public void onTextMsg(String jsonStr)
   {
      Gson gson = new GsonBuilder().serializeNulls().create();

      try {
         jsonStr = jsonStr.trim();
         Logger.debug("Rcv.:" + jsonStr + " from: " + thisSession.getId());
         Message clientMsg = gson.fromJson(jsonStr, Message.class);

         if (clientMsg.TYPE.equals("ACCOUNT")) {
            handleAccount(clientMsg);
         }

         validateUserSession();

         if (clientMsg.TYPE.equals("PING")) {
            sendPong();
         }

         if (clientMsg.TYPE.equals("CHAT")) {
            handleChat(clientMsg);
         }

      } catch (JsonSyntaxException e) {
         Logger.error(e);
      }
   }


   @OnClose
   public void onClose()
   {
      Logger.debug("onClose(): " + thisSession.getId());
      // Handle close without logout (peer went down due to network trouble etc.)
      if (thisSession.getUserProperties().containsKey("USER")) {
         logoutUser();
      }
   }


   private void sendPong()
   {
      Message pongMsg = new Message();
      pongMsg.TYPE = "PONG";
      sendMessage(pongMsg);
   }

   private void handleAccount(final Message clientMsg)
   {
      Message serverMsg = new Message();
      serverMsg.TYPE = "ACCOUNT";

      if (clientMsg.SUBTYPE.equals("LOGIN")) {
         serverMsg.SUBTYPE = "LOGIN";
         serverMsg.RESULT_MSG = loginUser(clientMsg.LOGIN_MSG.USER, clientMsg.LOGIN_MSG.PASSWD);

         if (thisSession.getUserProperties().containsKey("USER")) {
            LoginMsg loginMsg = new LoginMsg();
            loginMsg.USER = (String) thisSession.getUserProperties().get("USER");
            serverMsg.LOGIN_MSG = loginMsg;
            serverMsg.STATS_MSG = usersLoggedIn.get() + " User" + (usersLoggedIn.get() > 1 ? "s " : " ") + "online!";
         }
      }

      if (clientMsg.SUBTYPE.equals("LOGOUT")) {
         serverMsg.SUBTYPE = "LOGOUT";
         serverMsg.RESULT_MSG = logoutUser();
      }

      sendMessage(serverMsg);
   }


   private void handleChat(final Message clientMsg)
   {
      Message broadcastMsg;

      if (clientMsg.SUBTYPE.equals("MSG")) {
         ChatMsg chatMsg = new ChatMsg();
         broadcastMsg = clientMsg;
         // You can't trust nobody ;)
         chatMsg.MSG = clientMsg.CHAT_MSG.MSG.replace("<", "&lt;").replace("&", "&amp;");
         chatMsg.COLOR = (String) thisSession.getUserProperties().get("COLOR");
         chatMsg.FROM = (String) thisSession.getUserProperties().get("USER");
         broadcastMsg.CHAT_MSG = chatMsg;
         broadcastMessage(broadcastMsg, true);
      }
   }


   private void sendMessage(final Message serverMsg)
   {
      final Gson gson = new Gson();
      final String jsonStr = gson.toJson(serverMsg);

      try {
         if (thisSession.isOpen()) {
            Logger.debug("Send: " + jsonStr + " to: " + thisSession.getId());
            thisSession.getBasicRemote().sendText(jsonStr);
         }
      } catch (IOException e) {
         Logger.error(e);
      }
   }

   private void broadcastMessage(final Message serverMsg, final boolean includeThis)
   {
      final Gson gson = new Gson();
      final String jsonStr = gson.toJson(serverMsg);

      for (Session session : thisSession.getOpenSessions()) {
         if (!includeThis && thisSession.equals(session)) {
            continue;
         }
         session.getAsyncRemote().sendText(jsonStr);
      }
   }


   private ResultMsg loginUser(final String userName, final String password)
   {
      Logger.debug("LOGIN User: " + userName + " PW: " + password);
      User user;
      Message joinMsg = null;
      ResultMsg resultMsg = new ResultMsg();

      resultMsg.CODE = "ERR";

      for (Session s : thisSession.getOpenSessions()) {
         if (s.getUserProperties().containsKey("USER")) {
            String sessionUserName = (String) (s.getUserProperties().get("USER"));
            if (sessionUserName.equalsIgnoreCase(userName)) {
               resultMsg.MSG = "You are already logged in!";
               return resultMsg;
            }
         }
      }

      user = userDao.findByUserName(userName);

      if (user.getUsername() != null) {
         Logger.debug("User " + user.getUsername() + " does exist");
         if (PasswordStore.isPasswordCorrect(password, user.getPwHash())) {
            Logger.debug("Password OK");
            String userColor = "";
            int userNb = usersLoggedIn.incrementAndGet();
            int sessionId = Integer.parseInt(thisSession.getId(), 16);

            resultMsg.CODE = "OK";
            resultMsg.MSG = "Login successful!";

            thisSession.getUserProperties().put("USER", user.getUsername());

            // If a user is active more than once, give him the same color:
            if (userColorMap.containsKey(user.getUsername())) {
               userColor = userColorMap.get(user.getUsername());
            } else {
               userColor = PEER_COLORS[sessionId % PEER_COLOR_NB];
               userColorMap.put(user.getUsername(), userColor);
            }

            thisSession.getUserProperties().put("COLOR", userColor);

            joinMsg = new Message();
            joinMsg.TYPE = "INFO";
            joinMsg.SUBTYPE = "JOIN";
            joinMsg.INFO_MSG = user.getUsername() + " has entered the building";
            joinMsg.STATS_MSG = userNb + " User" + (userNb > 1 ? "s " : " ") + "online!";

            broadcastMessage(joinMsg, false);

            return resultMsg;
         }
      }

      resultMsg.MSG = "Wrong username or password!";
      return resultMsg;
   }


   private ResultMsg logoutUser()
   {
      ResultMsg resultMsg = new ResultMsg();

      if (thisSession.getUserProperties().containsKey("USER")) {
         int userNb = usersLoggedIn.decrementAndGet();

         resultMsg.CODE = "OK";
         resultMsg.MSG = "You have successfully logged out!";

         Message unjoinMsg = new Message();
         unjoinMsg.TYPE = "INFO";
         unjoinMsg.SUBTYPE = "JOIN";
         unjoinMsg.INFO_MSG = thisSession.getUserProperties().get("USER") + " has left the building";
         unjoinMsg.STATS_MSG = userNb + " User" + (userNb > 1 ? "s " : " ") + "online!";

         broadcastMessage(unjoinMsg, false);

         thisSession.getUserProperties().clear();
      } else {
         resultMsg.CODE = "ERR";
         resultMsg.MSG = "You where not logged in!";
      }

      return resultMsg;
   }

   /**
    * Validate this session
    *
    * @return "true" only if connection is open AND authorized
    */
   private boolean validateUserSession()
   {
      boolean result = false;

      // Close connection to unauthorized peers
      if (thisSession.isOpen()) {
         if (!thisSession.getUserProperties().containsKey("USER")) {
            Logger.debug("Closing connection to unauthorized peer: " + thisSession.getId());
            activeClose(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Message from unauthorized peer"));
            result = false;
         } else {
            result = true;
         }
      }

      return result;
   }

   private void activeClose(CloseReason reason)
   {
      try {
         if (thisSession.isOpen()) {
            Logger.debug("Closing connection to peer: " + thisSession.getId());
            thisSession.close(reason);
         }
      } catch (IOException e) {
         Logger.error(e);
      }
   }

   @Override
   public void contextInitialized(ServletContextEvent servletContextEvent)
   {
      Logger.debug(" executed");
   }

   @Override
   public void contextDestroyed(ServletContextEvent servletContextEvent)
   {
      Logger.debug(" executed");
      HibernateUtil.shutdown();
   }
}
