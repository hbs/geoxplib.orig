package com.geocoord.client.widgets.poipanel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;

public class PoiPanel extends Composite implements ClickHandler {

  public interface Resources extends ClientBundle {
    @Source("POIPanel.css")
    PoiPanelCssResource css();
  }
  
  @UiTemplate("POIPanel.ui.xml")
  interface POIPanelUiBinder extends UiBinder<FlowPanel, PoiPanel> {}
 
  private static final POIPanelUiBinder uiBinder = GWT.create(POIPanelUiBinder.class);
  
  @UiField Resources resources;
  @UiField FlowPanel root;
  @UiField Button add;
  
  public PoiPanel() {
    
    initWidget(uiBinder.createAndBindUi(this));
    resources.css().ensureInjected();
  }
  
  @UiHandler({"button"})
  @Override
  public void onClick(ClickEvent arg0) {
    //
    // Create a new PoiWidget and add it to the panel
    //
  }
}
