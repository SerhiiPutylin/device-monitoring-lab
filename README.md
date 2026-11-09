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
- Client виконує `flush()` після кожного повідомлення. Server збирає повідомлення
  до `\n` через `NewlineMessageReader` над buffered UTF-8 reader, тому framing
  не залежить від меж TCP reads.
- EOF завершує читання; listening і accepted sockets закриваються через
  `try-with-resources`. Client завершує цикл після interrupt під час паузи.

Task 01.1 пройшов статичне code review та локальні ручні перевірки автора.
У Task 01.2 додано read timeout, ліміт довжини повідомлення та відхилення
незавершеного повідомлення при EOF. Автор підтвердив ручні boundary checks.
Автоматизовані unit та loopback integration tests пройшли на JDK 21:
20 test cases, без failures, errors або skipped tests. Heartbeat і визначення
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
- Accepted socket має read timeout `5000 ms`. Це обмеження blocking read,
  а не загальний deadline отримання повідомлення. Очікування в `accept()`
  не має timeout.
- Максимальна довжина повідомлення — 256 Java `char` (UTF-16 code units),
  без `\n`. Це не ліміт UTF-8 bytes. Перевищення закриває сесію.
- Непорожній залишок повідомлення при EOF відхиляється з діагностикою.
  Timeout також завершує сесію; reconnect відсутній.
- Reconnect, heartbeat та визначення offline-стану ще не реалізовані.

Роботу з кількома connections, concurrency та загальним message deadline
заплановано пізніше.

## Автоматизовані тести

Використовується JUnit Jupiter 6.1.3 у `test` scope та Maven Surefire 3.5.5.
Запуск усіх тестів із кореня checkout:

```sh
mvn test
```

Лише unit tests для framing:

```sh
mvn -Dtest=NewlineMessageReaderTest test
```

`NewlineMessageReaderTest` перевіряє повідомлення та EOF, межу `256/257`,
кілька повідомлень у потоці, підрахунок Java `char` та поширення read errors.
Oversized input перевіряється reader, який не дозволяє дочекатися EOF.

`TcpServerIntegrationTest` використовує реальні sockets лише на loopback,
динамічні порти (`port 0`) та короткі read timeouts. Покрито 10 повідомлень
в одному write, UTF-8 через кілька writes, межу довжини, partial EOF,
мовчазне connection та закриття accepted socket. Кілька writes не гарантують
окремих TCP reads; assertions перевіряють результат framing.

Тести не потребують окремого запуску server або client і не використовують
порт `9000`. Для futures, socket operations і завершення worker задано
timeouts; `Thread.sleep()` для синхронізації не використовується.
Maven може завантажувати test dependencies та build plugins під час першого
запуску. Результати зберігаються в `target/surefire-reports/`.

Якщо `mvn` відсутній у `PATH`, можна запустити Maven lifecycle `test` через
Maven tool window в IntelliJ IDEA, використовуючи bundled Maven і JDK 21.

## Правила роботи

Основна реалізація виконується автором проєкту; AI допомагає з поясненнями,
архітектурними обговореннями та code review. Правила безпеки й межі роботи
описані в [AGENTS.md](AGENTS.md).
