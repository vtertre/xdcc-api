package fr.xdcc.pi.web.resource;

import com.google.common.collect.Lists;
import fr.xdcc.pi.model.bot.MongoBot;
import fr.xdcc.pi.model.service.MongoBotService;
import fr.xdcc.pi.web.marshaller.Marshaller;
import fr.xdcc.pi.web.marshaller.MongoBotMarshaller;
import net.codestory.http.annotations.Get;

import java.util.List;
import java.util.Map;

public class MongoBotResource extends AbstractResource {

  private MongoBotService mongoBotService;

  public MongoBotResource() {
    mongoBotService = new MongoBotService();
  }

  @Get("/bot")
  public List list() {
    Marshaller<MongoBot> mongoBotMarshaller = new MongoBotMarshaller();
    Iterable<MongoBot> mongoBots = mongoBotService.list();
    List<Map<String, Object>> botRepresentationList = Lists.newArrayList();
    mongoBots.forEach(bot -> botRepresentationList.add(mongoBotMarshaller.marshall(bot)));
    return botRepresentationList;
  }

  @Get("/bot/:id")
  public Map show(String id) {
    Marshaller<MongoBot> mongoBotMarshaller = new MongoBotMarshaller();
    MongoBot mongoBot = mongoBotService.get(parseObjectId(id));
    // TODO : Missing bot
    return mongoBotMarshaller.marshall(mongoBot);
  }
}