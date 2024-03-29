# Сканнер ЦВЗ
## Введение
Сканер цифровых водяных знаков (далее по тексту именуемое как Сканер ЦВЗ) - это мобильное приложение для смартфонов на базе ОС Android, разработанное для сканирования документов, обнаружения встроенных в изоб- ражения цифровых водяных знаков.
С исходным кодом можно ознакомиться в репозитории.
В настоящем руководстве содержится подробное описание исходного кода для понимания внутреннего устройства, возможности возобновления и продолжения разработки мобильного приложения.

## Авторские права
Материалы, представленные в данном приложении и содержащиеся в данном руководстве, за исключением иконок и некоторых используемых библиотек, являются собственностью Фролова Олега Витальевича, студента НИУ ВШЭ, 3-го курса образовательной программы “Компьютерная безопасность”, и охраняются в соответствии с международным и местным законодательством, в том числе законами об авторском праве и смежных правах. Любое воспроизведение, копирование, публикация, дальнейшее распространение или публичный показ материалов, представленных в настоящем документе, допускается только после получения соответствующего письменного разрешения правообладателя.
Любое несанкционированное использование материалов настоящего руководства может привести к возникновению гражданской ответственности и уголовному преследованию нарушителя в соответствии с действующим законодательством.

## Описание и назначение мобильного приложения

Мобильное приложение Сканер ЦВЗ позволяет пользователям сканировать документы при помощи камеры мобильного устройства, избавляться на сканах от теней и шумов в рамках возможностей данного мобильного приложения, а также позволяет извлечь все прямоугольные изображения из отсканированного документа и проверить их на наличие встроенного цифрового водяного знака методом, основанным на дискретном преобразовании Фурье или на косинусном преобразовании, в соответствии с указанными в настройках ключом и длиной цифрового водяного знака.

## Реализация
### Использованные библиотеки:
- OpenCV, собранная под ОС Android.
- Android-Image-Cropper
- DocumentScanner
- Chaquopy

### Классы
#### Список классов проекта последней версии проекта:
- MainActivity
- GalleryActivity
- Scanner

## Что реализовано на данный момент
- [x] Сканирование документов с камеры мобильного устройства с автоматическим захватом границ документа
- [x] Импорт заранее отсканированного изображения из внутреннего хранилища устройства
- [x] Избавление от шумов методом размытия Гаусса и от теней методом нормализации тона освещенности на снимке
- [x] Поиск встроенных в изображения ЦВЗ методом преобразования Фурье и косинусного преобразования
- [x] Настройки ключа и генерируемого на стороне приложения ЦВЗ для процесса поиска встроенных в изображения ЦВЗ

## Версии
- 1.0.0 - реализован базовый макет с кнопками и базовой логикой.
- 1.1.0 - добавлена возможность обрезания и сканирования документа с камеры мобильного устройства.
- 1.2.0 - добавлено: поддержка OpenCV, извлечение прямоугольных областей, устранение шумов и теней на сканах, сделанных с помощью камеры мобильного устройства.
- 1.3.0 - улучшено: дизайн, логика работы приложения; исправлены некоторые баги и ошибки в работе приложения.
- 1.4.0 - добавлен макет функционала по поиску цифровых водяных знаков, встроенных в изображения.
- 1.4a1 - добавлено: реализация поиска и задел под извлечение ЦВЗ, встроенного методом косинусного преобразования.
- 1.4a2 - проведена доработка функционала, оптимизация некоторых участков кода.
- 1.4a3 - добавлено: реализация поиска и задел под извлечение ЦВЗ, встроенного методом преобразования Фурье.
