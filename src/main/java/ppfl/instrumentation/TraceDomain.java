package ppfl.instrumentation;

public class TraceDomain {
  public String traceclass;
  public String tracemethod;
  public String signature;

  public TraceDomain(String traceclass, String tracemethod, String signature) {
    this.traceclass = traceclass.trim();
    this.tracemethod = tracemethod.trim();
    this.signature = signature.trim();
  }

  @Override
  public String toString() {
    return this.traceclass + ":" + this.tracemethod + ":" + this.signature;
  }

  @Override
  public boolean equals(Object oth) {
    if (oth == null || !(oth instanceof TraceDomain))
      return false;
    if (this == oth)
      return true;
    TraceDomain instance = (TraceDomain) oth;
    return traceclass.equals(instance.traceclass) && tracemethod.equals(instance.tracemethod)
        && signature.equals(instance.signature);
  }

  @Override
  public int hashCode() {
    int result = this.tracemethod.hashCode();
    result = result * 31 + this.traceclass.hashCode();
    result = result * 31 + this.signature.hashCode();
    return result;
  }
}
