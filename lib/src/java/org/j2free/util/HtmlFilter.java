//*****************************
//
// Author: Ryan Wilson
//
//******************************
package org.j2free.util;

import java.util.*;
import java.util.regex.*;
import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.logging.*;

@NotThreadSafe
public class HtmlFilter
{
    private final int PLAINTEXT_LINE_LENGTH = 60;

    private TreeSet<String> okayTags;
    private final String[] defaultAllowedTags = {
                                                   "br", "hr", "strong",
                                                    "b", "i", "u", "font",
                                                    "ul", "ol", "li", "a"
                                                };

    private final String HTML_START = "<([A-Z][A-Z0-9]*)\\b[^>]*>";
    private final String HTML_END   = "</([A-Z][A-Z0-9]*)\\b[^>]*>";

    private final String NOBR_SPACE_PATTERN = "&nbsp;";
    private final String EMAIL_LINE_BREAKS   = "</?[tb]r\\s?/?>";
    private final String LINK_PATTERN = "<a.*?href=\"([^\"]*?)\"[^>]*?>([^<]*?)</a>";

    public HtmlFilter()
    {
        okayTags = new TreeSet<String>();
        okayTags.addAll(Arrays.asList(defaultAllowedTags));
    }

    private final Log log = LogFactory.getLog(HtmlFilter.class);

    public String filter(String msg)
    {
        return filter(msg, defaultAllowedTags);
    }

    public String filter(String msg, String[] allowedTags)
    {
        if (msg == null)
            return "";
        else if (msg.length() == 0 || msg.equals("") || !msg.contains("<"))
            return msg;

        okayTags.clear();
        if (allowedTags != null && allowedTags.length > 0)
            okayTags.addAll(Arrays.asList(allowedTags));

        StringBuffer line = new StringBuffer(msg);
        for (int i = 0; i < line.length(); i++)
        {
            if (line.charAt(i) == '<')
            {
                int begin = i, end = i;
                for (int j = i + 1; j < line.length(); j++)
                {
                    if (line.charAt(j) == '>')
                    {
                        end = j + 1;
                        break;
                    }
                }
                if (!isOkayTag(new String(line.substring(begin, end))))
                {
                    line = line.delete(begin, end);
                }
            }
        }
        return new String(line);
    }

    private boolean isOkayTag(String tag)
    {
        int end = (tag.indexOf(" ") >= 0) ? tag.indexOf(" ") : tag.length() - 1;
        return (okayTags.contains(tag.substring(1, end)) || okayTags.contains(tag.substring(2, end)));
    }

    public String strictFilter(String text)
    {
        if (text == null || text.equals(""))
            return text;

        Pattern p0 = Pattern.compile(HTML_START, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        Pattern p1 = Pattern.compile(HTML_END, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

        Matcher m0 = p0.matcher(text);
        Matcher m1 = p1.matcher(m0.replaceAll(""));
        return m1.replaceAll("");
    }

    public String filterForEmail(String text)
    {
        Pattern links = Pattern.compile(LINK_PATTERN, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

        Matcher matcher = links.matcher(text);
        text = matcher.replaceAll("$2 [link: $1]");

        // First replace <tr> and <br> with \n
        text = text.replaceAll(EMAIL_LINE_BREAKS, "\n");

        text = strictFilter(text);
        
        text = text.replaceAll(NOBR_SPACE_PATTERN," ") // make all &nbsp; into plain text spaces
                   .replaceAll("\t"," ")               // make all tabs spaces
                   .replaceAll(" {2,}"," ")            // make all multiple space sections a single space
                   .replaceAll("^\\s","")              // remove any leaving space
                   .replaceAll("\n{2,}","\n\n");       // make sure there aren't too many line breaks in a row

        // Get the text as lines
        String[] lines  = text.split("\n");
        String[] words;

        StringBuilder body = new StringBuilder();

        int lineLength = 0;
        String lastWord;
        for (String line : lines)
        {
            words = line.split("\\s");
            lineLength = 0;

            lastWord = "";
            for (String word : words)
            {
                if (lineLength > 0 && lineLength + word.length() >= PLAINTEXT_LINE_LENGTH && !lastWord.equals("[link:"))
                {
                    body.append("\n" + word);
                    lineLength = word.length();
                } 
                else
                {
                    body.append((lineLength > 0 ? " " : "") + word);
                    lineLength += word.length();
                }
                lastWord = word;
            }
            body.append("\n");
        }
        return body.toString().replaceAll("\n{3,}","\n\n");
    }
    
    /*
    public static void main(String[] args) {
        String htmlText = "<html>" +
                      "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=\"utf-8\" />" +
                      "<body>" +
                      "<table width=\"500\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" bgcolor=\"#ffffff\">" +
                      "<tr>" +
                      "<td style=\"font-family: Arial, sans-serif; font-size: 24px; color: #111;\">" +
                      "<a href=\"http://www.jamlegend.com\">" +
                      "<img src=\"http://static.jamlegend.com/img/logo-small.gif\" alt=\"JamLegend\" title=\"Jam=Legend\" galleryimg=\"no\" width=\"200\" height=\"74\" border=\"0\" />" +
                      "</a>" +
                      "</td>" +
                      "</tr>" +
                      "<tr>" +
                      "<td style=\"font-family: Arial, sans-serif; font-size: 14px; color: #111;line-hei=ght:18px;\">Hi ryan,<br />" +
                      "<br />" +
                      "<br />" +
                      "<a href=\"http://www.jamlegend.com/user/Sneak!\">Sneak!</a> has challenged you to a duel on JamLegend. To win this duel, you'll need to beat <a href=\"http://www.jamlegend.com/user/Sneak!\">Sneak!</a>'s score of 193,464 points on <a href=\"http://www.jamlegend.com/song/66/174\">Young and Empty</a> by <a href=\"http://www.jamlegend.com/artist/60\">Blue Judy</a>.<br />" +
                      "<br />We think this is a great match up. Your best score is only 10,398 points less than <a href=\"http://www.jamlegend.com/user/Sneak!\">Sneak!</a>'s, so we think you've got a good shot.<br /><br />You can practice up first by clicking (or pasting into your browser) this link: <br />" +
                      "<a title=\"Practice Before Playing\" style=\"font-family:Arial,sans-serif;font-size:14px;text-decoration:underline;color:#14AFAE;\" href=\"http://www.jamlegend.com/song/66/174\">http://www.jamlegend.com/song/66/174</a>" +
                      "<br />" +
                      "<br />If you're all ready to go, you can play now by clicking (or pasting into your browser) this link:<br />" +
                      "<a title=\"Play Now\" style=\"font-family:Arial,sans-serif;font-size:14px;text-decoration:underline;color:#14AFAE;\" href=\"http://www.jamlegend.com/duellink/93bc9618037c2d7b4d11777a35c0dc6a/95543\">http://www.jamlegend.com/duellink/93bc9618037c2d7b4d11777a35c0dc6a/95543</a>" +
                      "<br />" +
                      "<br />Thanks for playing JamLegend,<br />- The JamLegend Team</td>" +
                      "</tr>" +
                      "<tr>" +
                      "<td height=\"30\" style=\"line-height:20px;height:20px;\">&nbsp;</td>" +
                      "</tr>" +
                      "<tr>" +
                      "<td style=\"font-family: Arial, san=s-serif; font-size: 12px; color: #999; text-align: left;\">" +
                      "<a href=\"http://www.jamlegend.com/edit/profile\" title=\"E-mail Preferences\" style=\"font-family: Arial, sans-serif; font-size: 11px; color: #999; text-decoration: underline;\">Unsubscribe</a>" +
                      "</td>" +
                      "</tr>" +
                      "</table>" +
                      "</body>" +
                      "</html>";

        HtmlFilter filter = new HtmlFilter();

        String plainText = filter.filterForEmail(htmlText);

        int i = 0;
        for (String line : plainText.split("\n")) {
            System.out.println(i + "\t" + line);
            i++;
        }
    }
     */
}