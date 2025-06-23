/**

The MIT License (MIT)

Copyright (c) 2022,2025, Robert Tykulsker

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

import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.config.ConfigurationKey;
import com.surftools.config.IConfigurationManager;
import com.surftools.miasma.messageService.IamSafeMessage;
import com.surftools.miasma.messageService.MessageProcessor;
import com.surftools.miasma.utils.FileUtils;

import io.javalin.http.Context;
import io.javalin.http.HttpCode;

/**
 *
 * @author bobt
 *
 */
public class EntryHandler extends AbstractBaseHandler {
  private static final Logger logger = LoggerFactory.getLogger(EntryHandler.class);

  private String rawHtml;

  private String replacementAddress = null;
  private final int MAX_SMS_CHAR_COUNT = 140;
  private final MessageProcessor mp;

  public EntryHandler(IConfigurationManager cm) {
    super(cm, logger);

    rawHtml = FileUtils.readFile(Path.of(cm.getAsString(ConfigurationKey.TEMPLATE_THANKS_FILE_NAME)), "thanks");
    replacementAddress = cm.getAsString(ConfigurationKey.APP_WINLINK_REPLACEMENT_MESSAGE_ADDRESS);

    if (replacementAddress.equals("(null)")) {
      replacementAddress = null;
    }
    mp = new MessageProcessor(cm);
  }

  @Override
  public void handle(Context ctx) throws Exception {
    super.handle(ctx);

    var now = LocalDateTime.now();

    var explanations = new ArrayList<String>();

    var name = getParam("name");
    var address = getParam("address");
    var message = getParam("message");
    var digits = address.replaceAll("[^\\d.]", "");
    var isEmail = digits.length() != 10;

    logger.info("got create request, name: " + name + ", address: " + address + ", message: " + message);

    // extra/repeated validation
    if (isEmail) {

    } else {
      digits = address.replaceAll("[^\\d.]", "");

      if (digits.length() != 10) {
        explanations.add("expected 10 digits in phone number: " + address);
      }
      var charCount = "From ".length() + name.length() + "MM/YY ONE WAY MSG\n".length() + message.length();
      if (charCount > MAX_SMS_CHAR_COUNT) {
        explanations.add("Total text for SMS message too long");
      }
    }

    var messageId = generateMid(String.join(",", List.of(name, address, message, now.toString())));

    var outboundMessageAddress = replacementAddress;
    if (outboundMessageAddress == null) {
      if (isEmail) {
        outboundMessageAddress = address;
      } else {
        outboundMessageAddress = digits + "@sms.radiorelay.org";
      }
    }

    if (explanations.size() > 0) {
      returnResult(HttpCode.NOT_ACCEPTABLE, "Bad input:\n" + String.join("\n", explanations));
    }

    var iAmSafeMessage = new IamSafeMessage(ctx.req.getRemoteAddr(), messageId, name, address, message, now, isEmail,
        outboundMessageAddress);

    mp.process(iAmSafeMessage);

    var html = new String(rawHtml);
    html = html.replaceAll("<!-- FROM -->", iAmSafeMessage.from());
    html = html.replaceAll("<!-- TO -->", iAmSafeMessage.to());
    html = html.replaceAll("<!-- MESSAGE -->", iAmSafeMessage.message());
    returnHtml(html);
  }

  public static String generateMid(String string) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      String stringToHash = string + System.nanoTime();
      md.update(stringToHash.getBytes());
      byte[] digest = md.digest();
      Base32 base32 = new Base32();
      String encodedString = base32.encodeToString(digest);
      String subString = encodedString.substring(0, 12);
      return subString;
    } catch (Exception e) {
      throw new RuntimeException("could not generate messageId: " + e.getMessage());
    }
  }

}
