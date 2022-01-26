package io.goobi.viewer.model.cms.widgets.embed;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import io.goobi.viewer.model.cms.CMSPage;
import io.goobi.viewer.model.cms.widgets.CustomSidebarWidget;
import io.goobi.viewer.model.translations.TranslatedText;

@Entity
@DiscriminatorValue("CUSTOM")
public class CMSSidebarElementCustom extends CMSSidebarElement {

    @ManyToOne
    @JoinColumn(name = "custom_widget_id", nullable = false)
    private CustomSidebarWidget widget;
    
    public CMSSidebarElementCustom() {
    }
    
    public CMSSidebarElementCustom(CustomSidebarWidget widget, CMSPage owner) {
        super(widget.getType(), owner);
        this.widget = widget;
    }
    
    public CMSSidebarElementCustom(CMSSidebarElementCustom orig, CMSPage owner) {
        super(orig.getContentType(), owner);
        this.widget = orig.widget;
    }

    public CustomSidebarWidget getWidget() {
        return widget;
    }
    
    @Override
    public TranslatedText getTitle() {
        return widget.getTitle();
    }
}