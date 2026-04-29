# Project-specific ProGuard/R8 rules.
# Add keep rules here when libraries or reflection-based code require them.

# PDFBox references optional JPEG2000 encoder/decoder classes that are not
# packaged by pdfbox-android. They are only needed for JPX/JPEG2000 streams.
-dontwarn com.gemalto.jp2.JP2Decoder
-dontwarn com.gemalto.jp2.JP2Encoder
