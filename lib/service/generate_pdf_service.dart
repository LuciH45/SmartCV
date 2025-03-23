import 'dart:io';
import 'dart:typed_data';

import 'package:cam_scanner/service/scanned_document_repository.dart';
import 'package:image/image.dart' as Im;
import 'package:injectable/injectable.dart';
import 'package:pdf/widgets.dart' as pw;


@lazySingleton
class GeneratePdfService {
  final ScannedDocumentRepository _scannedDocumentRepository;

  const GeneratePdfService(this._scannedDocumentRepository);

  Future<void> generatePdfFromImages(List<String> imagePaths) async {
    final pdf = pw.Document();

    for (final element in imagePaths) {
      final Im.Image? decodedImage = Im.decodeImage(await File(element).readAsBytes());
      if (decodedImage == null) {
        throw Exception('Failed to decode image: $element');
      }
      final Im.Image image = decodedImage;
      final Uint8List imageFileBytes = Im.encodeJpg(image, quality: 60);

      pdf.addPage(
        pw.Page(
          build: (pw.Context context) {
            return pw.Center(
              child: pw.Image(
                  pw.MemoryImage(imageFileBytes)
              ),
            );
          },
        ),
      );
    }


    await _scannedDocumentRepository.saveScannedDocument(
      firstPageUri: imagePaths.first,
      document: await pdf.save(),
    );
  }
}
