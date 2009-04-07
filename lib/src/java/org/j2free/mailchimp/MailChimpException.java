/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * MailChimpException.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.mailchimp;

/**
 *
 * @author ryan
 */
public class MailChimpException extends Exception {

    // General System Errors
    public static final int ServerError_MethodUnknown       = -32601;
    public static final int ServerError_InvalidParameters   = -32602;
    public static final int Unknown_Exception               = -99;
    public static final int Zend_Uri_Exception              = -92;
    public static final int PDOException                    = -91;
    public static final int Avesta_Db_Exception             = -91;
    public static final int XML_RPC2_Exception              = -90;
    public static final int XML_RPC2_FaultException         = -90;
    public static final int Too_Many_Connections            = -50;
    public static final int ParseException                  = 0;

    // 100: User Related Errors
    public static final int User_Unknown                    = 100;
    public static final int User_Disabled                   = 101;
    public static final int User_DoesNotExist               = 102;
    public static final int User_NotApproved                = 103;
    public static final int Invalid_ApiKey                  = 104;
    public static final int User_UnderMaintenance           = 105;

    // 120: User - Action Related Errors
    public static final int User_InvalidAction              = 120;
    public static final int User_MissingEmail               = 121;
    public static final int User_CannotSendCampaign         = 122;
    public static final int User_MisingModuleOutbox         = 123;
    public static final int User_ModuleAlreadyPurchased     = 124;
    public static final int User_ModuleNotPurchased         = 125;
    public static final int User_NotEnoughCredit            = 126;
    public static final int MC_InvalidPayment               = 127;

    // 200: List Related Errors
    public static final int List_DoesNotExist               = 200;

    // 210: List - Basic Actions
    public static final int List_InvalidInterestFieldType   = 210;
    public static final int List_InvalidOption              = 211;
    public static final int List_InvalidUnsubMember         = 212;
    public static final int List_InvalidBounceMember        = 213;
    public static final int List_AlreadySubscribed          = 214;
    public static final int List_NotSubscribed              = 215;

    // 220: List - Import Related
    public static final int List_InvalidImport              = 220;
    public static final int MC_PastedList_Duplicate         = 221;
    public static final int MC_PastedList_InvalidImport     = 222;

    // 230: List - Email Related
    public static final int Email_AlreadySubscribed         = 230;
    public static final int Email_AlreadyUnsubscribed       = 231;
    public static final int Email_NotExists                 = 232;
    public static final int Email_NotSubscribed             = 233;

    // 250: List - Merge Related
    public static final int List_MergeFieldRequired         = 250;
    public static final int List_CannotRemoveEmailMerge     = 251;
    public static final int List_Merge_InvalidMergeID       = 252;
    public static final int List_TooManyMergeFields         = 253;
    public static final int List_InvalidMergeField          = 254;

    // 270: List - Interest Group Related
    public static final int List_InvalidInterestGroup       = 270;
    public static final int List_TooManyInterestGroups      = 271;

    // 300: Campaign Related Errors
    public static final int Campaign_DoesNotExist           = 300;
    public static final int Campaign_StatsNotAvailable      = 301;

    // 310: Campaign - Option Related Errors
    public static final int Campaign_InvalidAbsplit         = 310;
    public static final int Campaign_InvalidContent         = 311;
    public static final int Campaign_InvalidOption          = 312;
    public static final int Campaign_InvalidStatus          = 313;
    public static final int Campaign_NotSaved               = 314;
    public static final int Campaign_InvalidSegment         = 315;

    // 330: Campaign - Ecomm Errors
    public static final int Invalid_EcommOrder              = 330;

    // 350: Campaign - Absplit Related Errors
    public static final int Absplit_UnknownError            = 350;
    public static final int Absplit_UnknownSplitTest        = 351;
    public static final int Absplit_UnknownTestType         = 352;
    public static final int Absplit_UnknownWaitUnit         = 353;
    public static final int Absplit_UnknownWinnerType       = 354;
    public static final int Absplit_WinnerNotSelected       = 355;

    // 500: Generic Validation Errors
    public static final int Invalid_Analytics               = 500;
    public static final int Invalid_DateTime                = 501;
    public static final int Invalid_Email                   = 502;
    public static final int Invalid_SendType                = 503;
    public static final int Invalid_Template                = 504;
    public static final int Invalid_TrackingOptions         = 505;
    public static final int Invalid_Options                 = 506;
    public static final int Invalid_Folder                  = 507;

    // 550: Generic Unknown Errors
    public static final int Module_Unknown                  = 550;
    public static final int MonthlyPlan_Unknown             = 551;
    public static final int Order_TypeUnknown               = 552;
    public static final int Invalid_PagingLimit             = 553;
    public static final int Invalid_PagingStart             = 554;

    private final int code;

    public MailChimpException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

}
