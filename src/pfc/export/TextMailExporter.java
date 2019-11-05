/*
 * Copyright (c) 2019 David Schmidt. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package pfc.export;

import java.io.*;
import java.text.*;
import java.util.Date;
import java.text.SimpleDateFormat;
import pfc.cab.*;

/**
 * Class to export mail messages to individual text/HTML files.
 *
 * @author David Schmidt <br>
 *         4 Nov 2019
 */
public class TextMailExporter implements Exporter {

   protected MboxFile mbox;
   protected int msgCount;
   protected String prefix;

   /** Creates a new instance of TextMailExporter */
   public TextMailExporter() {
      mbox = null;
      msgCount = 0;
   }

   /**
    * Sets file to receive exported items.
    */
   public void setFile(File exportFile) {
      prefix = exportFile.getParentFile().getPath() + File.separator;
   }

   /**
    * Returns true if cabinet item is valid for export. For mbox mail exports, the item must be a
    * mail envelope.
    */
   public boolean isExportable(CabinetItem item) {
      if ((item.getType() == CabinetItem.MAIL_ENVELOPE) && (item.getData() != 0)) {
         return true;
      } else {
         return false;
      }
   }

   /**
    * Opens export file.
    */
   public void open() throws IOException {}

   /**
    * Opens folder in export file. Does nothing here.
    */
   public void openFolder(CabinetItem item) {}

   /**
    * Exports cabinet item to export file. All mail information is taken from the mail item; the
    * envelope item is ignored.
    */
   public void export(CabinetItem envelope, CabinetItem item) throws IOException {
      // Create mail message by parsing item contents.
      PrintWriter out;
      String countPad = "";
      if (msgCount < 10)
         countPad = "00";
      else if (msgCount < 100)
         countPad = "0";
      MailMessage message = new MailMessage(item.getContent());
      Date date = message.getDate();
      if (message.isHtml()) {
         mbox = new MboxFile(new File(prefix + toSortableDate(date) + ".html"));
         out = mbox.getPrintWriter();
         out.println("<pre>");
      } else {
         mbox = new MboxFile(new File(prefix + toSortableDate(date) + ".txt"));
         out = mbox.getPrintWriter();
      }
      String attachment = message.getAttachment();
      // Write From line with date to file.
      if (date != null) {
         out.println("From - " + toAsctime(date));
      } else {
         out.println("From - " + message.getDateString());
      }
      // Write header, and blank line before body.
      out.println(message.getMailHeader());
      out.println();
      // Insert attachment name if available.
      if (attachment != null) {
         out.println("[" + attachment + "]");
      }
      if (message.isHtml()) {
         mbox = new MboxFile(new File(prefix + msgCount + ".html"));
         out.println("</pre>");
         out.println(indentFromInBody(message.toV7String(message.getBodyText().getBytes())));
      } else {
         // Write body, and blank line at end of message.
         out.println(indentFromInBody(message.getBodyText()));
      }
      out.println();
      out.flush();
      mbox.close();
      // Increment message count.
      msgCount++;
   }

   /**
    * Closes folder in export file. Does nothing here.
    */
   public void closeFolder() {}

   /**
    * Closes export file.
    */
   public void close() {}

   /**
    * Converts date to asctime date format. The result string is of the form: Sun Dec 08 13:59:59
    * 2002.
    */
   public static String toAsctime(Date date) {
      // Create formatter with output date pattern.
      SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy");
      return format.format(date);
   }

   /**
    * Converts date to sortable, file system friendly format. The result string is of the form: 
    * 2019-11-04T131512.0400.
    */
   public static String toSortableDate(Date date) {
      // Create formatter with output date pattern.
      SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HHmmss.SSSZ");
      return format.format(date);
   }

   /**
    * Indents lines in the message body that begin with 'From '. These lines are quoted by inserting
    * a &gt; character at the start. This distinguishes the lines from the first line of an mbox
    * mail message.
    */
   private String indentFromInBody(String body) {
      StringBuffer buffer = new StringBuffer(body);
      // Set up search string.
      String separator = System.getProperty("line.separator");
      String fromAtStart = new String(separator + "From ");
      // Search for occurrences of From at start of line.
      int pos = buffer.indexOf(fromAtStart);
      while (pos >= 0) {
         // Insert symbol to quote line.
         buffer.insert(pos + separator.length(), '>');
         pos = buffer.indexOf(fromAtStart, pos + fromAtStart.length());
      }
      return buffer.toString();
   }

}
