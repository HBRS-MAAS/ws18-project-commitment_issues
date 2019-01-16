package com.fxgraph.cells;


import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class TextCell extends Cell {
    private String parentID;
    private String content;
    private Text view;
    
    public TextCell(String cellId, String parentID, String content) {
      super(cellId);
      this.parentID = parentID;
      this.content = content;
      this.view = new Text();
      view.setText(this.content);
      view.setAccessibleText(this.content);
      view.setFont(new Font(20));
      
      setView(view);
      
      cellType = CellType.TEXT;
    }
    public String getParentID() {
      return parentID;
    }
    public void setParentID(String parentID) {
      this.parentID = parentID;
    }
    
    public String getContent() {
    	return this.content;
    }
    public void setContent(String content) {
      this.content = content;
      
    ((Text)getView()).setText(this.content);
  	((Text)getView()).setAccessibleText(this.content);
      
      //v.setText(this.content);
//      setView(v);
    }
    
    
    
  }

