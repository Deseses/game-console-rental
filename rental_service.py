from datetime import date
from decimal import Decimal, ROUND_HALF_UP, InvalidOperation

ACTIVE_STATUSES = {"Подтверждён", "Выдан"}


def is_available(console_id, start_date, end_date, orders):
    """Проверяет, свободна ли консоль в заданном периоде."""
    if start_date >= end_date:
        raise ValueError("Дата начала должна быть раньше даты окончания")

    for order in orders:
        if order["console_id"] != console_id:
            continue
        if order["status"] not in ACTIVE_STATUSES:
            continue

        # Проверка пересечения полуоткрытых периодов аренды.
        overlap = (
            start_date < order["end_date"]
            and order["start_date"] < end_date
        )
        if overlap:
            return False

    return True


def calculate_rent(price_per_day, start_date, end_date):
    """Рассчитывает стоимость аренды с точностью до копеек."""
    if start_date >= end_date:
        raise ValueError("Некорректный период аренды")

    try:
        rate = Decimal(str(price_per_day))
    except (InvalidOperation, ValueError):
        raise ValueError("Суточная ставка должна быть числом") from None

    if not rate.is_finite() or rate <= 0:
        raise ValueError(
            "Суточная ставка должна быть положительным конечным числом"
        )

    days = (end_date - start_date).days
    total = rate * days
    return total.quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)


def process_return(order, console, is_complete, has_damage):
    """Обрабатывает возврат консоли и обновляет статусы."""
    if order["status"] != "Выдан":
        raise ValueError("Возврат возможен только для выданного заказа")

    if order["console_id"] != console["console_id"]:
        raise ValueError("Консоль не принадлежит указанному заказу")

    if is_complete and not has_damage:
        order["status"] = "Завершён"
        console["status"] = "Доступна"
        return "Оборудование принято без замечаний"

    order["status"] = "Возврат с замечаниями"
    console["status"] = "Обслуживание"
    return "Зафиксированы замечания; требуется проверка"


def main():
    console = {"console_id": 1, "status": "Выдан"}
    orders = [
        {
            "console_id": 1,
            "start_date": date(2026, 11, 8),
            "end_date": date(2026, 11, 11),
            "status": "Выдан",
        }
    ]

    requested_start = date(2026, 11, 12)
    requested_end = date(2026, 11, 15)

    print(
        "Консоль доступна:",
        is_available(1, requested_start, requested_end, orders),
    )
    print(
        "Стоимость аренды:",
        calculate_rent(500, requested_start, requested_end),
        "руб.",
    )

    return_order = {
        "console_id": 1,
        "status": "Выдан",
    }
    print(process_return(return_order, console, True, False))
    print("Статус заказа:", return_order["status"])
    print("Статус консоли:", console["status"])


if __name__ == "__main__":
    main()
