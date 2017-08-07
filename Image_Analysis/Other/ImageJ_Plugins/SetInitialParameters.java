
import java.util.ArrayList;


public class SetInitialParameters{
  private Double alpha = 15.0;
  private Double beta = 100.0;
  private Double gamma = 250.0;
  private Double weight = 0.5;
  private Double stretch = 100.0;
  private Double range = 1000.0;
  private Double maxSegmentLength = 1.0;
  public ArrayList<Double[]> initial_parameters = new java.util.ArrayList();

  public SetInitialParameters(){
    //
  }

  public ArrayList<Double[]> makeArray(){

    this.initial_parameters.add(this.alpha);
    this.initial_parameters.add(this.beta);
    this.initial_parameters.add(this.gamma);
    this.initial_parameters.add(this.weight);
    this.initial_parameters.add(this.stretch);
    this.initial_parameters.add(this.range);
    this.initial_parameters.add(this.maxSegmentLength);

    return this.initial_parameters;
  }
  // public static void main(String[] args){
  //   System.out.print("Hello World!");
  // }
}
