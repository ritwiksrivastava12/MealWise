package in.mealwise.api.orders;

import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrderStateTest {
  @Test void illegalTransitionRejected() {
    assertFalse(Orders.FLOW.get("CREATED").contains("DELIVERED"));
    assertTrue(Orders.FLOW.get("PAYMENT_PENDING").contains("PAID"));
  }
  @Test void terminalStatesHaveNoExit() {
    assertEquals(Set.of(), Orders.FLOW.get("DELIVERED"));
  }
}
