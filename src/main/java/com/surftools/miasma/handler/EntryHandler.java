/**

The MIT License (MIT)

Copyright (c) 2025, Robert Tykulsker

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.


*/

package com.surftools.miasma.handler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.config.ConfigurationKey;
import com.surftools.config.IConfigurationManager;
import com.surftools.miasma.messageService.InboundMessage;
import com.surftools.miasma.messageService.MessageWriter;

import io.javalin.http.Context;
import io.javalin.http.HttpCode;

/**
 *
 * @author bobt
 *
 */
public class EntryHandler extends AbstractBaseHandler {
  private static final Logger logger = LoggerFactory.getLogger(EntryHandler.class);

  private AtomicInteger sequenceGenerator;
  private Path sequencePath;

  private MessageWriter messageWriter;

  public EntryHandler(IConfigurationManager cm) throws Exception {
    super(cm, logger, ConfigurationKey.TEMPLATE_THANKS_FILE_NAME);

    var seqPathString = cm.getAsString(ConfigurationKey.SERVER_SEQUENCE_GENERATOR_PATH);
    if (seqPathString == null) {
      throw new RuntimeException(
          "No value for configuration: " + ConfigurationKey.SERVER_SEQUENCE_GENERATOR_PATH.name());
    }
    sequencePath = Path.of(seqPathString);
    var seqParent = sequencePath.getParent();
    var seqDir = seqParent.toFile();
    if (!seqDir.exists()) {
      Files.createDirectories(Path.of(seqDir.toString()));
      sequenceGenerator = new AtomicInteger(0);
    } else {
      var seqString = Files.readString(sequencePath).trim();
      sequenceGenerator = new AtomicInteger(Integer.valueOf(seqString));
    }
    logger.info("Sequence Generator set at: " + sequenceGenerator.get());

    messageWriter = new MessageWriter(cm);

  }

  @Override
  public void handle(Context ctx) throws Exception {
    super.handle(ctx);

    var now = LocalDateTime.now();
    var sequenceNumber = sequenceGenerator.incrementAndGet();
    Files.writeString(sequencePath, String.valueOf(sequenceNumber));

    var name = getParam("name");
    var address = getParam("address");
    var message = getParam("message");

    logger.info("got request name: " + name + ", address: " + address + ", message: " + message);

    var inboundMessage = new InboundMessage(now, sequenceNumber, ctx.req.getRemoteAddr(), //
        name, address, message);

    var explanations = extraValidation(inboundMessage);
    if (explanations.size() > 0) {
      returnResult(HttpCode.NOT_ACCEPTABLE, "Bad input:\n" + String.join("\n", explanations));
    }

    // var outboundMessages = makeOutboundMessages(inboundMessage);
    // messageWriter.writeOutboundMessages(outboundMessages);
    messageWriter.write(inboundMessage);

    var html = getTemplateHtml();
    html = html.replaceAll("<!-- FROM -->", inboundMessage.from());
    html = html.replaceAll("<!-- TO -->", inboundMessage.to());
    html = html.replaceAll("<!-- MESSAGE -->", inboundMessage.message());
    returnHtml(html);
  }

  // private List<OutboundMessage> makeOutboundMessages(InboundMessage inboundMessage) {
  // var list = new ArrayList<OutboundMessage>();
  // if (inboundMessage.isEmail()) {
  // var outboundMessage = new OutboundMessage(inboundMessage, smsType);
  // list.add(outboundMessage);
  // return list;
  // }
  //
  // if (smsType != SmsType.RAINBOW) {
  // list.add(new OutboundMessage(inboundMessage, smsType));
  // } else {
  // for (var type : SmsType.RAINBOX_LIST) {
  // list.add(new OutboundMessage(inboundMessage, type));
  // }
  // }
  //
  // return list;
  // }

  private List<String> extraValidation(InboundMessage inboundMessage) {
    var explanations = new ArrayList<String>();
    return explanations;
  }

}
