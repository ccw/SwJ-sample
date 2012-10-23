package com.sas.custom.servlet;

import com.sas.svcs.directives.client.AbstractDirectiveUrlCreator;

import java.util.List;

/**
 *
 */
public class EGCMainDirective extends AbstractDirectiveUrlCreator {

    public EGCMainDirective() {
        super.setDirective("EGCMain");
    }

    @Override
    protected List onValidate() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
