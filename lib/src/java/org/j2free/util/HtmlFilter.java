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
}