import lombok.Upsortable;
import java.util.Set;
import lombok.UpsortableSets;
import lombok.UpsortableSet;
import lombok.UpsortableValue;
@lombok.Upsortable class UpsortablePlain implements UpsortableValue {
  int i;
  int foo;
  UpsortablePlain() {
    super();
  }
  public @java.lang.SuppressWarnings("all") @javax.annotation.Generated("lombok") @lombok.Generated void setI(final int i) {
    if ((this.i == i))
        return ;
    try
      {
        final String fname = this.getClass().getDeclaredField("i").getName();
        final Set<UpsortableSet> set = lombok.UpsortableSets.getGlobalUpsortable().get(((this.getClass().getName() + ".") + fname));
        UpsortableSet[] participatingSets = new UpsortableSet[set.size()];
        int found = 0;
        for (UpsortableSet upsortableSet : set) 
          if (upsortableSet.remove(this))
              {
                found = (found + 1);
                participatingSets[found] = upsortableSet;
              }
        this.i = i;
        int localI = 0;
        while ((localI < found))          {
            participatingSets[localI].add(this);
            localI = (localI + 1);
          }
      }
    catch (final NoSuchFieldException e)
      {
        e.printStackTrace();
      }
  }
  public @java.lang.SuppressWarnings("all") @javax.annotation.Generated("lombok") @lombok.Generated void setFoo(final int foo) {
    if ((this.foo == foo))
        return ;
    try
      {
        final String fname = this.getClass().getDeclaredField("foo").getName();
        final Set<UpsortableSet> set = lombok.UpsortableSets.getGlobalUpsortable().get(((this.getClass().getName() + ".") + fname));
        UpsortableSet[] participatingSets = new UpsortableSet[set.size()];
        int found = 0;
        for (UpsortableSet upsortableSet : set) 
          if (upsortableSet.remove(this))
              {
                found = (found + 1);
                participatingSets[found] = upsortableSet;
              }
        this.foo = foo;
        int localI = 0;
        while ((localI < found))          {
            participatingSets[localI].add(this);
            localI = (localI + 1);
          }
      }
    catch (final NoSuchFieldException e)
      {
        e.printStackTrace();
      }
  }
}