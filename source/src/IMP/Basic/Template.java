package IMP.Basic;

import AntlrGen.HMLParser;

import java.util.List;

/**
 * Created by fofo on 2014/9/29.
 */
public class Template {
    List formalVarNames;
    HMLParser.TemplateContext templateContext;

    public Template(List formalVarNames, HMLParser.TemplateContext templateContext){
        this.formalVarNames = formalVarNames;
        this.templateContext = templateContext;
    }

    public HMLParser.TemplateContext getTemplateContext() {
        return templateContext;
    }
}
