package com.fxgraph.cells;

import javafx.scene.control.Button;

public class TextCell extends Cell {
    private String parentID;
    private String content;
    public TextCell(String cellId, String parentID, String content) {
      super(cellId);
      this.parentID = parentID;
      this.content = content;
      Button view = new Button();
      view.setText(content);
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
    }
    
    
    
  }

