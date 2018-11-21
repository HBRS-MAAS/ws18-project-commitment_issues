package com.fxgraph.cells;

import javafx.scene.control.Button;

public class TextCell extends Cell {
    private String parentID;
    private String content;
    private Button view;
    
    public TextCell(String cellId, String parentID, String content) {
      super(cellId);
      this.parentID = parentID;
      this.content = content;
      view = new Button();
      view.setText(this.content);
      setView(view);
    }
    public String getParentID() {
      return parentID;
    }
    public void setParentID(String parentID) {
      this.parentID = parentID;
    }
    public void setContent(String content) {
      this.content = content;
      
      view.setText(this.content);
      setView(view);
    }
    
    
    
  }

