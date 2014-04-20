package fr.xdcc.pi.tasker.bot;

import fr.xdcc.pi.tasker.service.TaskerService;
import org.jibble.pircbot.PircBot;
import org.jibble.pircbot.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Custom implementation of PircBot.
 * This solution is not final and will be removed when the issue
 * with FileTransfer of PircBotX library will be fixed
 */
public class SentryBot extends PircBot {

  private static final Logger LOG = LoggerFactory.getLogger(SentryBot.class);

  private TaskerService taskerService;

  private String[] senderBotTags;

  public SentryBot(String name) {
    setName(name);
    setLogin(name);
    setAutoNickChange(true);
    taskerService = new TaskerService();
    Properties properties = new fr.xdcc.pi.tasker.bot.Properties().load();
    senderBotTags = properties.getProperty("xdcc.sender-tag").split(",");
  }

  @Override
  protected void onUserList(String channel, User[] users) {
    assert channel.equals("#serial_us");

    List<User> userList = Arrays.asList(users);
    List<String> senderBotNameList = userList.parallelStream().filter(this::isBotSender).map(
        user -> user.getNick().substring(1)
    ).collect(Collectors.toList());

    if (!senderBotNameList.isEmpty()) {
      taskerService.updateAvailableBots(senderBotNameList);
    } else {
      LOG.debug("No bot sender found on {}", channel);
    }

    if (isConnected()) {
      disconnect();
      dispose();
    }
  }

  @Override
  protected void onPrivateMessage(String senderNick, String login, String hostname, String message) {
    LOG.info("Message from <{}>: [{}]", senderNick, message);
  }

  @Override
  protected void onConnect() {
    LOG.info("SentryBot connected to server");
  }

  @Override
  protected void onDisconnect() {
    LOG.debug("SentryBot disconnected from server");
  }

  private boolean isBotSender(User u) {
    for (String tag : senderBotTags) {
      if (u.getNick().startsWith(tag)) {
        return true;
      }
    }

    return false;
  }
}