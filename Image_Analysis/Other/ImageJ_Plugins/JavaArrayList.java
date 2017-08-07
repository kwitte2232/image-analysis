
import java.util.ArrayList;

public class SetInitialParameters{
  private Double alpha;
  private Double beta;
  private Double gamma;
  private Double weight;
  private Double stretch;
  private Double range;
  private Double maxSegmentLength;

  private ArrayList<Double[]> makeArray(){
    this.alpha = 15.0;
    this.beta = 100.0;
    this.gamma = 250.0;
    this.weight = 0.5;
    this.stretch = 100.0;
    this.range = 1000.0;
    this.maxSegmentLength = 1.0;

    ArrayList initial_parameters = new ArrayList();
    initial_parameters.add(this.alpha);
    initial_parameters.add(this.beta);
    initial_parameters.add(this.gamma);
    initial_parameters.add(this.weight);
    initial_parameters.add(this.stretch);
    initial_parameters.add(this.range);
    initial_parameters.add(this.maxSegmentLength);

    return initial_parameters;
  }
}
