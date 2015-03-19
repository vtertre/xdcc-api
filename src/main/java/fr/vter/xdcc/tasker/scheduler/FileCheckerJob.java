package fr.vter.xdcc.tasker.scheduler;

import fr.vter.xdcc.model.MongoBot;
import fr.vter.xdcc.infrastructure.persistence.mongo.MongoBotService;
import fr.vter.xdcc.tasker.bot.FileListUpdaterBot;
import fr.vter.xdcc.tasker.service.TaskerService;
import org.jibble.pircbot.IrcException;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;

// The setters are required for the injection of the job parameters
@SuppressWarnings("UnusedDeclaration")
@PersistJobDataAfterExecution
public class FileCheckerJob implements Job {

  @Inject
  public FileCheckerJob(TaskerService taskerService, MongoBotService mongoBotService) {
    this.taskerService = taskerService;
    this.mongoBotService = mongoBotService;
  }

  @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    long mongoBotsCount = mongoBotService.count();
    long remainingBotsToUpdate = mongoBotsCount - offset;
    long nbTasksToAchieve = remainingBotsToUpdate < OFFSET_INCREMENT ?
        remainingBotsToUpdate : OFFSET_INCREMENT;

    FileListUpdaterBot fileListUpdaterBot = new FileListUpdaterBot(taskerService, (int) nbTasksToAchieve);
    // fileListUpdaterBot.setVerbose(true);

    LOG.info("Starting job: FileCheckerJob");
    try {
      fileListUpdaterBot.connect("irc.otaku-irc.fr");
      fileListUpdaterBot.joinChannel("#serial_us");
      synchronized (fileListUpdaterBot) {
        fileListUpdaterBot.wait();
      }
    } catch (IOException | IrcException e) {
      LOG.debug("Failed to connect the bot to the server: {}", e.getMessage());
    } catch (InterruptedException e) {
      LOG.warn(e.getMessage());
    }

    Iterable<MongoBot> mongoBots = mongoBotService.paginate(max, offset);
    mongoBots.forEach(bot -> fileListUpdaterBot.sendMessage(bot.getName(), "xdcc send -1"));

    int nextOffset = (max + offset) >= mongoBotsCount ? 0 : (offset + OFFSET_INCREMENT);
    jobExecutionContext.getJobDetail().getJobDataMap().put("offset", nextOffset);
    LOG.info("End of job: FileCheckerJob");
  }

  public void setMax(int max) {
    this.max = max;
  }

  public void setOffset(int offset) {
    this.offset = offset;
  }

  private final TaskerService taskerService;
  private final MongoBotService mongoBotService;
  private int max;
  private int offset;
  private static final int OFFSET_INCREMENT = 5;
  private static final Logger LOG = LoggerFactory.getLogger(FileCheckerJob.class);
}