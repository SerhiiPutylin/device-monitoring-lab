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
  Початкове підключення має connect timeout `1000 ms` і максимум три спроби,
  із секундними паузами лише між невдалими спробами.
- `server.TcpServer` слухає `127.0.0.1:9000`, обслуговує з'єднання послідовно
  і виводить кожне отримане повідомлення в консоль.
- Обидві сторони явно використовують UTF-8; delimiter повідомлення — `\n`.
- Client виконує `flush()` після кожного повідомлення. Server збирає повідомлення
  до `\n` через `NewlineMessageReader` над buffered UTF-8 reader, тому framing
  не залежить від меж TCP reads.
- EOF завершує читання; listening і accepted sockets закриваються через
  `try-with-resources`. Client завершує цикл після interrupt під час паузи.

Task 01.1 пройшов статичне code review та локальні ручні перевірки автора.
У Task 01.2 додано read timeout, ліміт довжини повідомлення та відхилення
незавершеного повідомлення при EOF. Автор підтвердив ручні boundary checks.
У Task 01.3 додано bounded retries початкового connection для `ConnectException`
і `SocketTimeoutException`; interrupt під час паузи припиняє подальші спроби.
У Task 01.4 listener продовжує приймати клієнтів після EOF, read timeout або
protocol error попередньої сесії.
Автоматизовані unit та loopback integration tests пройшли на JDK 21:
36 test cases, без failures, errors або skipped tests. Heartbeat і визначення
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
робить секундну паузу й закриває connection. Server отримує EOF і чекає
наступного клієнта. Для повторної сесії знову запусти лише client.
Щоб зупинити server, натисни Ctrl+C у терміналі або Stop в IntelliJ IDEA.

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

- Blocking I/O; accept loop обслуговує одну клієнтську сесію за раз.
- Accepted socket має read timeout `5000 ms`. Це обмеження blocking read,
  а не загальний deadline отримання повідомлення. Очікування в `accept()`
  не має timeout.
- Максимальна довжина повідомлення — 256 Java `char` (UTF-16 code units),
  без `\n`. Це не ліміт UTF-8 bytes. Перевищення закриває сесію.
- Непорожній залишок повідомлення при EOF відхиляється з діагностикою.
  Read timeout також завершує сесію.
- Client повторює лише початкове підключення, максимум три рази загалом.
  `Connection refused` може виникнути раніше за connect timeout.
  Помилка надсилання telemetry завершує роботу без нового connection.
- Відновлення вже встановленої сесії, heartbeat та визначення offline-стану
  ще не реалізовані.

Одночасне обслуговування кількох сесій та загальний message deadline
заплановано пізніше.

## Автоматизовані тести

Використовується JUnit Jupiter 6.1.3 у `test` scope та Maven Surefire 3.5.5.
Запуск усіх тестів із кореня checkout:

```sh
mvn test
```

Лише unit tests для framing та client retries:

```sh
mvn -Dtest=NewlineMessageReaderTest,DeviceSimulatorTest test
```

`NewlineMessageReaderTest` перевіряє повідомлення та EOF, межу `256/257`,
кілька повідомлень у потоці, підрахунок Java `char` та поширення read errors.
Oversized input перевіряється reader, який не дозволяє дочекатися EOF.

`TcpServerIntegrationTest` використовує реальні sockets лише на loopback,
динамічні порти (`port 0`) та короткі read timeouts. Покрито 10 повідомлень
в одному write, UTF-8 через кілька writes, межу довжини, partial EOF,
мовчазне connection та закриття accepted socket. Кілька writes не гарантують
окремих TCP reads; assertions перевіряють результат framing.

`SequentialSessionsIntegrationTest` перевіряє, що той самий listener приймає
другого клієнта й обробляє його повідомлення після EOF, read timeout,
завеликого або незавершеного повідомлення першого клієнта.

`DeviceSimulatorTest` контролює створення sockets і паузи без зовнішньої мережі.
Покрито успіх на першій, другій і третій спробах, refusal та connect timeout,
вичерпання retry budget без зайвої паузи, закриття failed sockets до паузи,
збереження interrupt flag, нумерацію спроб та відсутність reconnect після
помилки telemetry. Connect timeout моделюється контрольованим socket,
а не підключенням до випадкової зовнішньої адреси.

`DeviceSimulatorIntegrationTest` перевіряє справжній client connection через
loopback: 10 telemetry messages з правильними sequence numbers та EOF після
закриття client. Пауза підмінена, щоб тест не чекав 10 секунд.

Тести не потребують окремого запуску server або client і не використовують
порт `9000`. Для futures, socket operations і завершення worker задано
timeouts; `Thread.sleep()` для синхронізації не використовується.
Interrupt-сценарії ізольовані в окремих worker threads, щоб не змінювати
interrupt status JUnit thread. Повний suite також перевірено offline
(`mvn --offline test`) з уже завантаженими залежностями.
Maven може завантажувати test dependencies та build plugins під час першого
запуску. Результати зберігаються в `target/surefire-reports/`.

Якщо `mvn` відсутній у `PATH`, можна запустити Maven lifecycle `test` через
Maven tool window в IntelliJ IDEA, використовуючи bundled Maven і JDK 21.

## Правила роботи

Основна реалізація виконується автором проєкту; AI допомагає з поясненнями,
архітектурними обговореннями та code review. Правила безпеки й межі роботи
описані в [AGENTS.md](AGENTS.md).
