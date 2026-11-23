import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ArrayList;

public class RentalService {
    enum ConsoleStatus {
        AVAILABLE, RESERVED, RENTED, MAINTENANCE
    }

    enum OrderStatus {
        CONFIRMED, ISSUED, COMPLETED, RETURN_ISSUES
    }

    interface Returnable {
        String processReturn(
            RentalOrder order,
            Console console,
            boolean complete,
            boolean damaged
        );
    }

    static class Console {
        private final int id;
        private final String model;
        private final BigDecimal dailyPrice;
        private ConsoleStatus status;

        Console(int id, String model, BigDecimal dailyPrice) {
            if (dailyPrice == null || dailyPrice.signum() <= 0) {
                throw new IllegalArgumentException(
                    "Цена должна быть положительной"
                );
            }

            this.id = id;
            this.model = model;
            this.dailyPrice = dailyPrice;
            this.status = ConsoleStatus.AVAILABLE;
        }

        int getId() {
            return id;
        }

        BigDecimal getDailyPrice() {
            return dailyPrice;
        }

        ConsoleStatus getStatus() {
            return status;
        }

        void setStatus(ConsoleStatus status) {
            this.status = status;
        }

        String getModel() {
            return model;
        }
    }

    static class RentalOrder {
        private final int id;
        private final Console console;
        private final LocalDate startDate;
        private final LocalDate endDate;
        private OrderStatus status;

        RentalOrder(
            int id,
            Console console,
            LocalDate start,
            LocalDate end,
            OrderStatus status
        ) {
            if (console == null || start == null || end == null
                    || !start.isBefore(end)) {
                throw new IllegalArgumentException(
                    "Некорректные данные заказа"
                );
            }

            this.id = id;
            this.console = console;
            this.startDate = start;
            this.endDate = end;
            this.status = status;
        }

        int getId() {
            return id;
        }

        int getConsoleId() {
            return console.getId();
        }

        LocalDate getStartDate() {
            return startDate;
        }

        LocalDate getEndDate() {
            return endDate;
        }

        OrderStatus getStatus() {
            return status;
        }

        void setStatus(OrderStatus status) {
            this.status = status;
        }

        long getDays() {
            return ChronoUnit.DAYS.between(startDate, endDate);
        }
    }

    static class RentalManager implements Returnable {
        boolean isAvailable(
            int consoleId,
            LocalDate start,
            LocalDate end,
            List<RentalOrder> orders
        ) {
            if (start == null || end == null || !start.isBefore(end)) {
                throw new IllegalArgumentException("Некорректный период");
            }

            for (RentalOrder order : orders) {
                if (order.getConsoleId() != consoleId) {
                    continue;
                }

                if (order.getStatus() != OrderStatus.CONFIRMED
                        && order.getStatus() != OrderStatus.ISSUED) {
                    continue;
                }

                boolean overlap = start.isBefore(order.getEndDate())
                    && order.getStartDate().isBefore(end);

                if (overlap) {
                    return false;
                }
            }

            return true;
        }

        BigDecimal calculateRent(
            Console console,
            LocalDate start,
            LocalDate end
        ) {
            if (console == null || start == null || end == null
                    || !start.isBefore(end)) {
                throw new IllegalArgumentException("Некорректный период");
            }

            long days = ChronoUnit.DAYS.between(start, end);
            return console.getDailyPrice()
                .multiply(BigDecimal.valueOf(days))
                .setScale(2, RoundingMode.HALF_UP);
        }

        @Override
        public String processReturn(
            RentalOrder order,
            Console console,
            boolean complete,
            boolean damaged
        ) {
            if (order == null || console == null) {
                throw new IllegalArgumentException(
                    "Заказ и консоль должны быть указаны"
                );
            }

            if (order.getStatus() != OrderStatus.ISSUED) {
                throw new IllegalStateException(
                    "Заказ не находится в статусе выдачи"
                );
            }

            if (order.getConsoleId() != console.getId()) {
                throw new IllegalArgumentException(
                    "Консоль не принадлежит заказу"
                );
            }

            if (complete && !damaged) {
                order.setStatus(OrderStatus.COMPLETED);
                console.setStatus(ConsoleStatus.AVAILABLE);
                return "Оборудование принято без замечаний";
            }

            order.setStatus(OrderStatus.RETURN_ISSUES);
            console.setStatus(ConsoleStatus.MAINTENANCE);
            return "Зафиксированы замечания; требуется проверка";
        }
    }

    public static void main(String[] args) {
        Console console = new Console(
            1,
            "PlayStation 5",
            new BigDecimal("500.00")
        );

        RentalManager manager = new RentalManager();
        LocalDate start = LocalDate.of(2026, 11, 8);
        LocalDate end = LocalDate.of(2026, 11, 11);

        RentalOrder order = new RentalOrder(
            1,
            console,
            start,
            end,
            OrderStatus.ISSUED
        );

        List<RentalOrder> orders = new ArrayList<>();
        orders.add(order);

        System.out.println(
            "Доступна на выбранный период: "
            + manager.isAvailable(
                1,
                LocalDate.of(2026, 11, 11),
                LocalDate.of(2026, 11, 14),
                orders
            )
        );
        System.out.println(
            "Стоимость аренды: "
            + manager.calculateRent(console, start, end)
            + " руб."
        );
        System.out.println(
            manager.processReturn(order, console, true, false)
        );
        System.out.println("Статус заказа: " + order.getStatus());
        System.out.println("Статус консоли: " + console.getStatus());
    }
}
