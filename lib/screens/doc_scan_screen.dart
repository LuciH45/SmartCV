import 'dart:io';

import 'package:cam_scanner/widgets/loading_overlay.dart';
import 'package:cam_scanner/service/generate_pdf_service.dart';
import 'package:edge_detection/edge_detection.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:carousel_slider/carousel_slider.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:get_it/get_it.dart';
import 'package:r_dotted_line_border/r_dotted_line_border.dart';

class DocScanScreen extends StatefulWidget {
  @override
  _DocScanScreenState createState() => _DocScanScreenState();
}

class _DocScanScreenState extends State<DocScanScreen> {
  final List<String> _documents = [];

  @override
  void dispose() {
    super.dispose();
    for (final document in _documents) {
      File(document).delete();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Scanning document'),
      ),
      body: SizedBox(
        width: double.infinity,
        height: double.infinity,
        child: CarouselSlider.builder(
          itemCount: _documents.length + 1,
          itemBuilder: (BuildContext context, int index, int realIndex) {
            if (index < _documents.length) {
              return Stack(
                alignment: Alignment.center,
                children: [
                  Image.file(
                    File(_documents[index]),
                  ),
                  RawMaterialButton(
                    onPressed: () {
                      File(_documents[index])..delete();
                      _documents.removeAt(index);
                      setState(() {});
                    },
                    elevation: 0.0,
                    fillColor: const Color(0x50000000),
                    child: const Icon(
                      FontAwesomeIcons.trash,
                      size: 35.0,
                    ),
                    padding: const EdgeInsets.all(15.0),
                    shape: const CircleBorder(),
                  ),
                ],
              );
            } else {
              return InkWell(
                onTap: () {
                  EdgeDetection.detectEdge('/path/to/save/image.jpg').then((value) {
                    if (value == true) {
                      const savedImagePath = '/path/to/save/image.jpg';
                      _documents.add(savedImagePath);
                      setState(() {});
                    }
                  });
                },
                child: Container(
                  margin: const EdgeInsets.symmetric(
                    vertical: 50,
                  ),
                  decoration: BoxDecoration(
                    border: RDottedLineBorder.all(
                      width: 1,
                      dottedLength: 6,
                      dottedSpace: 10,
                    ),
                  ),
                  child: const Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(
                          FontAwesomeIcons.plus,
                          size: 35,
                        ),
                        SizedBox(
                          height: 15,
                        ),
                        Text(
                          'Add new image',
                          style: TextStyle(
                            fontSize: 25,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              );
            }
          },
          options: CarouselOptions(
            height: double.infinity,
            viewportFraction: 0.8,
            enlargeCenterPage: true,
          ),
        ),
      ),
      bottomNavigationBar: Material(
        elevation: 4,
        color: Theme.of(context).primaryColor,
        child: SafeArea(
          child: Container(
            padding: const EdgeInsets.only(
              bottom: 10,
              top: 10,
              right: 15,
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                ElevatedButton(
                  onPressed: () {
                    final generatePdfService =
                        GetIt.I.get<GeneratePdfService>();

                    LoadingOverlay.of(context)
                        .during(
                      generatePdfService.generatePdfFromImages(_documents),
                    )
                        .then((value) {
                      Navigator.of(context).pop();
                    });
                  },
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Theme.of(context).colorScheme.secondary,
                  ),
                  child: const Text('CONTINUE'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
