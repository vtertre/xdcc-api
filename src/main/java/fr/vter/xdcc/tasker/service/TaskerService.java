package fr.vter.xdcc.tasker.service;

import com.google.common.collect.Sets;
import fr.vter.xdcc.infrastructure.persistence.mongo.MongoBotService;
import fr.vter.xdcc.model.Bot;
import fr.vter.xdcc.model.ConcreteFile;
import fr.vter.xdcc.model.MongoBot;
import fr.vter.xdcc.tasker.parser.XdccListFileParser;
import fr.vter.xdcc.tasker.parser.XdccWebsiteParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class TaskerService {

  @Inject
  public TaskerService(MongoBotService mongoBotService) {
    this.mongoBotService = mongoBotService;
  }

  public void updateAvailableFiles(File updatedList, String botNickname) {
    Map<Long, String> packMap = xdccListFileParser.parse(updatedList);

    Bot bot = mongoBotService.findByName(botNickname);
    if (bot == null) {
      bot = new MongoBot(botNickname);
    }

    internalUpdate(bot, packMap);
  }

  public void updateAvailableFiles(String botNickname) {
    Bot bot = mongoBotService.findByName(botNickname);
    String botStringUrl;

    if (bot == null) {
      bot = new MongoBot(botNickname);
      bot.setUrl(botStringUrl = urlFinder.findBotUrl(botNickname));
    } else {
      botStringUrl = (bot.getUrl() != null) ? bot.getUrl() : urlFinder.findBotUrl(botNickname);
    }

    if (botStringUrl == null) {
      LOG.debug("URL of bot {} could not be found", botNickname);
      return;
    }

    try {
      URL url = new URL(botStringUrl);
      Map<Long, String> packMap = xdccWebsiteParser.parse(url.openStream());
      bot.setUrl(botStringUrl);
      internalUpdate(bot, packMap);
    } catch (IOException exception) {
      LOG.info(
          "There was a problem with the URL [{}] of bot {} ==> [{}]",
          botStringUrl,
          botNickname,
          exception.getMessage()
      );
      bot.setUrl(null);
      mongoBotService.update((MongoBot) bot);
    }
  }

  public void updateAvailableBots(List<String> botNameList) {
    Iterable<MongoBot> savedBotList = mongoBotService.getBotsIn(botNameList);
    savedBotList.forEach(bot -> botNameList.remove(bot.getName()));

    List<MongoBot> botToUpdateList = botNameList.stream().map(MongoBot::new).collect(Collectors.toList());

    botToUpdateList.stream().forEach(mongoBotService::insert);
  }

  private void internalUpdate(Bot bot, Map<Long, String> packMap) {
    Set<ConcreteFile> concreteFileSet = Sets.newHashSet();

    // TODO Meh..
    packMap.entrySet().stream().forEach(entry ->
            concreteFileSet.add(new ConcreteFile(entry.getKey(), entry.getValue()))
    );

    if (!concreteFileSet.equals(bot.getFileSet())) {
      bot.setFileSet(concreteFileSet);
      bot.setLastUpdated(new Date());
      LOG.info("Bot {} got new files", bot.getName());
    } else {
      LOG.info("Files of bot {} remain unchanged", bot.getName());
    }

    bot.setLastChecked(new Date());
    mongoBotService.update((MongoBot) bot);
  }

  private MongoBotService mongoBotService;
  private UrlFinder urlFinder = new UrlFinder();
  private XdccListFileParser xdccListFileParser = new XdccListFileParser();
  private XdccWebsiteParser xdccWebsiteParser = new XdccWebsiteParser();
  private static final Logger LOG = LoggerFactory.getLogger(TaskerService.class);
}