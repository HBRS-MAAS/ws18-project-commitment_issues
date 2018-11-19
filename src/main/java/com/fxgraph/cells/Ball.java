package com.fxgraph.cells;

import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class Ball extends Cell{
  public Ball( String id) {
    super( id);

    Circle view = new Circle( 30);

    view.setStroke(Color.DODGERBLUE);
    view.setFill(Color.YELLOW);

    setView( view);

}
}