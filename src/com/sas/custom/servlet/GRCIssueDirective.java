package com.sas.custom.servlet;

import com.sas.svcs.directives.client.AbstractDirectiveUrlCreator;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: twncwc
 * Date: 2011/12/29
 * Time: 上午 10:17
 * To change this template use File | Settings | File Templates.
 */
public class GRCIssueDirective extends AbstractDirectiveUrlCreator {

    public GRCIssueDirective() {
        super.setDirective("GRC_Issue");
    }

    @Override
    protected List onValidate() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
