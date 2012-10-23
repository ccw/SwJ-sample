package com.sas.custom.servlet;

import com.sas.svcs.directives.client.AbstractDirectiveUrlCreator;

import java.util.List;

public class WRSDirective extends AbstractDirectiveUrlCreator {
    public static final String WRS_VIEW_REPORT_DIRECTIVE_NAME = "BIViewReport";
    public static final String WRS_LOGON_DIRECTIVE = "WRSLogon";
    public static final String WRS_DEFAULT_SW_COMPONENT_NAME = "SAS Web Report Studio 4.3";

    public WRSDirective() {
        super.setDirective(getViewReportDirective());
    }

    protected String getViewReportDirective() {
        return "BIViewReport";
    }

    protected List onValidate() {
        return null;
    }
}