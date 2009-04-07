package org.j2free.mailchimp;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author ryan
 */
public class MailChimpList implements Serializable {

    private static final long serialVersionUID = 11l;

    private String listId;
    private String name;

    private int webId;
    private int size;

    private Date created;

    // Jan 16, 2009 03:03 pm
    private static final SimpleDateFormat MC_DATEFORMAT = new SimpleDateFormat("MMM dd, yyyy hh:mm a");

    public MailChimpList() {
    }

    public MailChimpList(String listId, String name, int webId, int size, String created) throws ParseException {

        this.listId  = listId;
        this.name    = name;
        this.webId   = webId;
        this.size    = size;
        this.created = MC_DATEFORMAT.parse(created);
    }

    public MailChimpList(JSONObject json) throws JSONException, ParseException {
        
        this(   json.getString("id"),
                json.getString("name"),
                json.getInt("web_id"),
                json.getInt("member_count"),
                json.getString("date_created")
            );
        
    }

    /**
     * @return the listId
     */
    public String getListId() {
        return listId;
    }

    /**
     * @param listId the listId to set
     */
    public void setListId(String listId) {
        this.listId = listId;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the webId
     */
    public int getWebId() {
        return webId;
    }

    /**
     * @param webId the webId to set
     */
    public void setWebId(int webId) {
        this.webId = webId;
    }

    /**
     * @return the size
     */
    public int getSize() {
        return size;
    }

    /**
     * @param size the size to set
     */
    public void setSize(int size) {
        this.size = size;
    }

    /**
     * @return the created
     */
    public Date getCreated() {
        return created;
    }

    /**
     * @param created the created to set
     */
    public void setCreated(Date created) {
        this.created = created;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;

        if (!(other instanceof MailChimpList))
            return false;

        MailChimpList otherList = (MailChimpList)other;

        if (this.listId != null && this.listId.equals(otherList.listId))
            return true;

        return false;
    }

    @Override
    public int hashCode() {
        return this.listId != null ? this.listId.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "MailChimpList[listId=" + listId + ",webId=" + webId + ",name=" + name + ",size=" + size + ",created=" + created + "]";
    }
}
