# device-monitoring-lab

Навчальний Java-проєкт для розвитку навичок networking, distributed systems,
real-time communication та IoT-adjacent backend engineering.

## Навчальні цілі

- Розібратися з обміном повідомленнями між device simulator і backend.
- Навчитися працювати з concurrency, timeouts і втратою з'єднання.
- Практикувати fault tolerance, resource management і локальне тестування.
- Поступово поглиблювати знання Linux і мережевої діагностики.

## Початковий навчальний сценарій

Заплановано: локальний device simulator надсилає heartbeat, а backend визначає
доступність пристрою за часом останнього отриманого повідомлення. Перший крок —
один simulated device і перевірка переходу в offline після припинення heartbeat.

## Поточний стан

Task 01.1 — TCP Client & Server: реалізовано мінімальний обмін telemetry між
одним device simulator і TCP server на Java 21, без Spring Boot і сторонніх
networking frameworks.

- `client.DeviceSimulator` надсилає 10 повідомлень із секундними паузами.
- `server.TcpServer` слухає `127.0.0.1:9000`, приймає одне з'єднання і виводить
  кожне отримане повідомлення в консоль.
- Обидві сторони явно використовують UTF-8; delimiter повідомлення — `\n`.
- Client виконує `flush()` після кожного повідомлення. Server використовує
  `BufferedReader.readLine()`, тому framing не залежить від меж TCP reads.
- EOF завершує читання; listening і accepted sockets закриваються через
  `try-with-resources`. Client завершує цикл після interrupt під час паузи.

Статичне code review Task 01.1 завершено. Локальна runtime-перевірка ще
не підтверджена; автоматизованих тестів поки немає. Heartbeat і визначення
offline-стану залишаються наступними етапами навчального сценарію.

## Підготовка до запуску

Потрібні JDK 21 і Maven. Maven має використовувати JDK 21; у `pom.xml`
встановлено `maven.compiler.release=21`. Maven Wrapper у проєкті відсутній.
Порт `9000` на loopback має бути вільним.

Наведені команди призначені для checkout із реалізацією Task 01.1
(`pom.xml` і `src/main/java`). Виконуй їх із кореня цього checkout.
Команди документовано за кодом; їх виконання в межах review не перевірялося.

Спочатку скомпілюй проєкт:

```sh
mvn compile
```

Перший build може потребувати завантаження Maven plugins.

У першому терміналі запусти server:

```sh
java -cp target/classes server.TcpServer
```

Server очікує connection без повідомлення про готовність у консолі.
У другому терміналі з того самого checkout запусти client:

```sh
java -cp target/classes client.DeviceSimulator
```

Очікуваний результат: у консолі server з'являться 10 окремих рядків із
sequence numbers від `0` до `9`. Після останнього повідомлення client ще
робить секундну паузу, закриває connection, а server отримує EOF і завершується.
Для повторної сесії спочатку знову запусти server, потім client.

В IntelliJ IDEA можна запускати `main()` класів `server.TcpServer` і
`client.DeviceSimulator` у такому самому порядку, використовуючи Project SDK 21.

## Формат повідомлень

```text
deviceId,sequenceNumber,temperature\n
```

Приклад одного повідомлення до delimiter:

```text
device_01,0,23.5
```

`deviceId` фіксований: `device_01`. Sequence number починається з `0` для
кожного запуску client. Temperature генерується випадково в діапазоні
`[0, 25)` і не округлюється. Server виводить рядок без parsing або validation.

## Відомі обмеження

- Blocking I/O та одна клієнтська сесія; accept loop і concurrency відсутні.
- Read timeout не задано: якщо client не надсилає delimiter і не закриває
  вихідний напрямок connection, server може чекати необмежено довго.
  `readLine()` також повертає останній непорожній рядок при EOF,
  навіть якщо він не завершується delimiter.
- Максимальна довжина повідомлення не обмежена.
- Reconnect, heartbeat та визначення offline-стану ще не реалізовані.

Timeouts і message size limits розглядатимемо в Task 01.2 — TCP Failure
Scenarios. Роботу з кількома connections і concurrency заплановано пізніше.

## Правила роботи

Основна реалізація виконується автором проєкту; AI допомагає з поясненнями,
архітектурними обговореннями та code review. Правила безпеки й межі роботи
описані в [AGENTS.md](AGENTS.md).
