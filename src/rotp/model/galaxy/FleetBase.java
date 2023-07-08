package rotp.model.galaxy;

import rotp.ui.main.GalaxyMapPanel;

public abstract class FleetBase {
  private static final long serialVersionUID = 1L;
  private float arrivalTime = Float.MAX_VALUE;

  private transient boolean displayed;

  public float arrivalTime() { return arrivalTime; }
  protected abstract float calculateArrivalTime();
  public void setArrivalTime() {
    arrivalTime = calculateArrivalTime();
  }

  public boolean displayed() { return displayed; }
  protected abstract boolean decideWhetherDisplayed(GalaxyMapPanel map);
  public void setDisplayed() {
    displayed = decideWhetherDisplayed();
  }
}
