import 'package:awesome_dialog/awesome_dialog.dart';
import 'package:flutter/material.dart';

class AlertModal {
  BuildContext _context;

  void alert({
    required String title,
    required String desc,
  }) {
    AwesomeDialog(
      context: _context,
      dialogType: DialogType.error,
      animType: AnimType.bottomSlide,
      title: title,
      desc: desc,
      btnOkOnPress: () {},
      btnOkText: 'Accept',
      btnOkColor: Colors.red,
    ).show();
  }

  AlertModal._create(this._context);

  factory AlertModal.of(BuildContext context) {
    return AlertModal._create(context);
  }
}