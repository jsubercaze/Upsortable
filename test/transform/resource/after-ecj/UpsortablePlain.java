import lombok.Upsortable;
@lombok.Upsortable class UpsortablePlain {
  int i;
  int foo;
  UpsortablePlain() {
    super();
  }
  public @java.lang.SuppressWarnings("all") @javax.annotation.Generated("lombok") void setI(final int i) {
    if ((this.i == i))
        return ;
    final java.lang.reflect.Field field = this.getClass().getDeclaredField("i");
  }
  public @java.lang.SuppressWarnings("all") @javax.annotation.Generated("lombok") void setFoo(final int foo) {
    if ((this.foo == foo))
        return ;
    final java.lang.reflect.Field field = this.getClass().getDeclaredField("foo");
  }
}
