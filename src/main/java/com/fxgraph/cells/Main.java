package com.fxgraph.cells;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;


public class Main extends Application {

  static Graph graph = new Graph();
  public Main(Graph g) {
    super();
    Main.graph = g;
  }
  public Main() {
    super();
  }
  public void setGraph(Graph g) {
    Main.graph = g;
  }
  @Override
  public void start(Stage primaryStage) throws InterruptedException{
      BorderPane root = new BorderPane();


      root.setCenter(graph.getScrollPane());

      Scene scene = new Scene(root, 2000, 2000);
      primaryStage.setScene(scene);
      primaryStage.show();
      
  }
  private void addGraphComponents(String id, CellType t) {
    Model model = graph.getModel();

    graph.beginUpdate();
    model.addCell(id, t);
    graph.endUpdate();
    model.getAllCells().get(0).relocate(100, 100);

  }
  

  public void main() throws InterruptedException {
    
    this.launch();
    
    
  }
}
