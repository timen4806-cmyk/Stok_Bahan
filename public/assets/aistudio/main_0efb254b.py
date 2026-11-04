import os
import sys
import sqlite3
from pathlib import Path

from PySide6.QtCore import Qt
from PySide6.QtWidgets import (
    QApplication, QMainWindow, QWidget, QVBoxLayout, QHBoxLayout,
    QTableWidget, QTableWidgetItem, QPushButton, QLineEdit, QLabel,
    QMessageBox, QInputDialog, QHeaderView, QAbstractItemView
)
from openpyxl import Workbook
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from PySide6.QtGui import QPixmap, QIcon
from reportlab.platypus import SimpleDocTemplate, Table, TableStyle, Paragraph


BAHAN_AWAL = [
    "PIPA 6mm", "PIPA 8mm", "PIPA 10mm", "PIPA 17mm",
    "HOLO 10x14", "HOLO 10x14 U", "HOLO 10x14 UT",
    "HOLO 10x18 U", "HOLO 10x18 UT", "HOLO 10x21",
    "HOLO 10x21 U", "HOLO 1/2 x 1/2", "HOLO BUNGA 15 U",
    "HOLO BUNGA 13 U", "HOLO BUNGA 15 T", "HOLO BUNGA 1' (BROWN)",
    "HOLO BUNGA 1' (GOLD)", "HOLO BUNGA 1' (WHITE)",
    "HOLO BUNGA 7/8 (BROWN)", "HOLO BUNGA 7/8 (GOLD)",
    "HOLO BUNGA 7/8 (WHITE)", "HOLO KOTAK 20 (CA)",
    "HOLO KOTAK 20 (BROWN)", "HOLO KOTAK 20 (GOLD)",
    "HOLO KOTAK 20 (WHITE)", "HOLO PINGGUL (BROWN)",
    "HOLO PINGGUL (GOLD)", "HOLO PINGGUL (WHITE)",
    "HOLO 22x34 (BROWN)", "HOLO 22x34 (GOLD)",
    "HOLO 22x34 (WHITE)", "OPENBACK (CA)", "OPENBACK (BROWN)",
    "OPENBACK (GOLD)", "OPENBACK (WHITE)",
    "HOLO 10x21 U (BROWN) UKIR", "HOLO 10x21 U (GOLD)",
    "HOLO 10x21 U (WHITE)", "PIPA ULIR (BROWN)",
    "PIPA ULIR (GOLD)", "PIPA ULIR (WHITE)", "LIS U 8mm",
    "LIS U 7mm", "LIS L 1/2", "SPIGOT", "HOLO KOTAK 14x14",
]


def app_data_dir():
    base = Path(os.getenv("APPDATA", Path.home()))
    folder = base / "StokBahan"
    folder.mkdir(parents=True, exist_ok=True)
    return folder


DB_PATH = app_data_dir() / "stok_bahan.db"

def resource_path(filename):
    base = getattr(sys, "_MEIPASS", os.path.dirname(os.path.abspath(__file__)))
    return os.path.join(base, filename)


class StokBahanApp(QMainWindow):
    def __init__(self):
        super().__init__()
        self.setWindowTitle("Stok Bahan — Manajemen Inventaris")
        self.resize(1200, 720)
        self.db = sqlite3.connect(DB_PATH)
        self.db.execute("PRAGMA foreign_keys = ON")
        self.setup_database()
        self.setup_ui()
        self.load_data()

    def setup_database(self):
        self.db.executescript("""
            CREATE TABLE IF NOT EXISTS bahan (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nama TEXT NOT NULL,
                nilai1 TEXT DEFAULT '',
                nilai2 TEXT DEFAULT '',
                nilai3 TEXT DEFAULT '',
                nilai4 TEXT DEFAULT '',
                nilai5 TEXT DEFAULT ''
            );
            CREATE TABLE IF NOT EXISTS pengaturan (
                id INTEGER PRIMARY KEY CHECK (id = 1),
                judul TEXT DEFAULT 'STOK BAHAN',
                kolom1 TEXT DEFAULT '',
                kolom2 TEXT DEFAULT '',
                kolom3 TEXT DEFAULT '',
                kolom4 TEXT DEFAULT '',
                kolom5 TEXT DEFAULT ''
            );
            INSERT OR IGNORE INTO pengaturan (id) VALUES (1);
        """)
        count = self.db.execute("SELECT COUNT(*) FROM bahan").fetchone()[0]
        if count == 0:
            self.db.executemany(
                "INSERT INTO bahan (nama) VALUES (?)",
                [(nama,) for nama in BAHAN_AWAL]
            )
        self.db.commit()

    def get_settings(self):
        return self.db.execute(
            "SELECT judul, kolom1, kolom2, kolom3, kolom4, kolom5 "
            "FROM pengaturan WHERE id=1"
        ).fetchone()

    def setup_ui(self):
        central = QWidget()
        self.setCentralWidget(central)
        root = QVBoxLayout(central)
        root.setContentsMargins(24, 22, 24, 22)
        root.setSpacing(14)

        title_row = QHBoxLayout()

        logo = QLabel()
        logo_path = resource_path("logo_sinar_mas.png")

        if os.path.exists(logo_path):
            logo.setPixmap(
                QPixmap(logo_path).scaled(
                    56,
                    56,
                    Qt.KeepAspectRatio,
                    Qt.SmoothTransformation
                )
            )
            title_row.addWidget(logo)

        title = QLabel("SINAR MAS ALUMINIUM")
        title.setObjectName("title")
        title_row.addWidget(title)

        title_row.addStretch()

        self.search = QLineEdit()
        self.search.setPlaceholderText("Cari nama bahan…")
        self.search.setFixedWidth(260)
        self.search.textChanged.connect(self.load_data)
        title_row.addWidget(self.search)

        root.addLayout(title_row)

        info = QLabel("Kelola daftar bahan dan catatan stok dalam satu aplikasi.")
        info.setObjectName("subtitle")
        root.addWidget(info)

        buttons = QHBoxLayout()
        self.add_button = self.make_button("＋  Tambah bahan", "primary")
        self.edit_button = self.make_button("Ubah nama", "secondary")
        self.delete_button = self.make_button("Hapus", "danger")
        self.header_button = self.make_button("Atur judul kolom", "secondary")
        self.pdf_button = self.make_button("Ekspor PDF", "secondary")
        self.excel_button = self.make_button("Ekspor Excel", "secondary")

        self.add_button.clicked.connect(self.add_item)
        self.edit_button.clicked.connect(self.edit_item)
        self.delete_button.clicked.connect(self.delete_item)
        self.header_button.clicked.connect(self.edit_headers)
        self.pdf_button.clicked.connect(self.export_pdf)
        self.excel_button.clicked.connect(self.export_excel)

        for button in (
            self.add_button, self.edit_button, self.delete_button,
            self.header_button, self.pdf_button, self.excel_button
        ):
            buttons.addWidget(button)
        buttons.addStretch()
        root.addLayout(buttons)

        self.table = QTableWidget()
        self.table.setColumnCount(7)
        self.table.setAlternatingRowColors(True)
        self.table.setSelectionBehavior(QAbstractItemView.SelectRows)
        self.table.setEditTriggers(
            QAbstractItemView.DoubleClicked | QAbstractItemView.EditKeyPressed
        )
        self.table.horizontalHeader().setSectionResizeMode(
            QHeaderView.ResizeToContents
        )
        self.table.horizontalHeader().setStretchLastSection(True)
        self.table.verticalHeader().setVisible(False)
        self.table.itemChanged.connect(self.save_cell)
        root.addWidget(self.table)

        self.setStyleSheet("""
            QMainWindow, QWidget { background: #f4f7fb; color: #172033;
                font-family: "Segoe UI"; font-size: 10pt; }
            QLabel#title { font-size: 24pt; font-weight: 700; color: #14213d; }
            QLabel#subtitle { color: #718096; font-size: 10pt; }
            QLineEdit { background: white; border: 1px solid #d8e0eb;
                border-radius: 8px; padding: 10px; }
            QTableWidget { background: white; border: 1px solid #e1e7ef;
                border-radius: 10px; gridline-color: #edf0f5; }
            QHeaderView::section { background: #eaf0f8; color: #263650;
                padding: 10px; border: none; font-weight: 700; }
            QTableWidget::item { padding: 7px; }
            QPushButton { padding: 10px 14px; border: none; border-radius: 8px;
                font-weight: 600; }
            QPushButton#primary { background: #2457d6; color: white; }
            QPushButton#primary:hover { background: #1746bd; }
            QPushButton#secondary { background: white; color: #27364f;
                border: 1px solid #d8e0eb; }
            QPushButton#secondary:hover { background: #edf3fb; }
            QPushButton#danger { background: #fff0f0; color: #bf3030; }
            QPushButton#danger:hover { background: #ffe2e2; }
        """)

    def make_button(self, text, style):
        button = QPushButton(text)
        button.setObjectName(style)
        return button

    def load_data(self, *_):
        self.table.blockSignals(True)
        settings = self.get_settings()
        headers = ["NO", "NAMA BAHAN"] + [
            text if text.strip() else f"KOLOM {i}"
            for i, text in enumerate(settings[1:], start=1)
        ]
        self.table.setHorizontalHeaderLabels(headers)

        query = self.search.text().strip()
        if query:
            rows = self.db.execute(
                """SELECT id, nama, nilai1, nilai2, nilai3, nilai4, nilai5
                   FROM bahan WHERE nama LIKE ? ORDER BY id""",
                (f"%{query}%",)
            ).fetchall()
        else:
            rows = self.db.execute(
                """SELECT id, nama, nilai1, nilai2, nilai3, nilai4, nilai5
                   FROM bahan ORDER BY id"""
            ).fetchall()

        self.table.setRowCount(len(rows))
        for row_index, record in enumerate(rows):
            item = QTableWidgetItem(str(row_index + 1))
            item.setData(Qt.UserRole, record[0])
            item.setFlags(item.flags() & ~Qt.ItemIsEditable)
            item.setTextAlignment(Qt.AlignCenter)
            self.table.setItem(row_index, 0, item)

            for col_index, value in enumerate(record[1:], start=1):
                self.table.setItem(row_index, col_index, QTableWidgetItem(value or ""))

        self.table.blockSignals(False)

    def selected_id(self):
        row = self.table.currentRow()
        if row < 0:
            return None
        return self.table.item(row, 0).data(Qt.UserRole)

    def add_item(self):
        name, ok = QInputDialog.getText(self, "Tambah bahan", "Nama bahan:")
        if ok and name.strip():
            self.db.execute("INSERT INTO bahan (nama) VALUES (?)", (name.strip(),))
            self.db.commit()
            self.load_data()

    def edit_item(self):
        row_id = self.selected_id()
        if row_id is None:
            QMessageBox.information(self, "Pilih data", "Pilih baris bahan terlebih dahulu.")
            return
        old_name = self.table.item(self.table.currentRow(), 1).text()
        name, ok = QInputDialog.getText(self, "Ubah nama bahan", "Nama bahan:", text=old_name)
        if ok and name.strip():
            self.db.execute("UPDATE bahan SET nama=? WHERE id=?", (name.strip(), row_id))
            self.db.commit()
            self.load_data()

    def delete_item(self):
        row_id = self.selected_id()
        if row_id is None:
            QMessageBox.information(self, "Pilih data", "Pilih baris bahan terlebih dahulu.")
            return
        answer = QMessageBox.question(
            self, "Konfirmasi hapus", "Hapus bahan yang dipilih?",
            QMessageBox.Yes | QMessageBox.No
        )
        if answer == QMessageBox.Yes:
            self.db.execute("DELETE FROM bahan WHERE id=?", (row_id,))
            self.db.commit()
            self.load_data()

    def edit_headers(self):
        settings = self.get_settings()
        values = []
        for i, current in enumerate(settings[1:], start=1):
            text, ok = QInputDialog.getText(
                self, "Judul kolom", f"Judul untuk kolom tambahan {i}:",
                text=current
            )
            if not ok:
                return
            values.append(text.strip())
        self.db.execute(
            """UPDATE pengaturan SET kolom1=?, kolom2=?, kolom3=?,
               kolom4=?, kolom5=? WHERE id=1""", values
        )
        self.db.commit()
        self.load_data()

    def save_cell(self, item):
        if item.column() == 0:
            return
        row_id = self.table.item(item.row(), 0).data(Qt.UserRole)
        if item.column() == 1:
            self.db.execute("UPDATE bahan SET nama=? WHERE id=?", (item.text(), row_id))
        else:
            field = f"nilai{item.column() - 1}"
            self.db.execute(f"UPDATE bahan SET {field}=? WHERE id=?", (item.text(), row_id))
        self.db.commit()

    def export_rows(self):
        settings = self.get_settings()
        headers = ["NO", "NAMA BAHAN"] + [
            value if value.strip() else f"KOLOM {i}"
            for i, value in enumerate(settings[1:], start=1)
        ]
        rows = self.db.execute(
            "SELECT nama, nilai1, nilai2, nilai3, nilai4, nilai5 FROM bahan ORDER BY id"
        ).fetchall()
        data = [headers]
        for number, row in enumerate(rows, start=1):
            data.append([number, *row])
        return settings[0], data

    def export_pdf(self):
        from PySide6.QtWidgets import QFileDialog
        path, _ = QFileDialog.getSaveFileName(
            self, "Simpan PDF", "stok_bahan.pdf", "PDF (*.pdf)"
        )
        if not path:
            return
        try:
            title, data = self.export_rows()
            doc = SimpleDocTemplate(
                path,
                pagesize=A4,
                rightMargin=18,
                leftMargin=18,
                topMargin=18,
                bottomMargin=18
            )

            table = Table(
                data,
                colWidths=[25, 130, 76, 76, 76, 76, 76],
                repeatRows=1
            )

            table.setStyle(TableStyle([
                ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#2457D6")),
                ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
                ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
                ("ALIGN", (0, 0), (0, -1), "CENTER"),
                ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
                ("GRID", (0, 0), (-1, -1), 0.3, colors.HexColor("#cbd5e1")),
                ("ROWBACKGROUNDS", (0, 1), (-1, -1),
                 [colors.white, colors.HexColor("#f1f5f9")]),
                ("FONTSIZE", (0, 0), (-1, -1), 6),
                ("LEADING", (0, 0), (-1, -1), 7),
                ("TOPPADDING", (0, 0), (-1, -1), 4),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
                ("LEFTPADDING", (0, 0), (-1, -1), 3),
                ("RIGHTPADDING", (0, 0), (-1, -1), 3),
            ]))
            doc.build([
                __import__("reportlab.platypus", fromlist=["Paragraph"]).Paragraph(
                    title or "STOK BAHAN",
                    __import__("reportlab.lib.styles", fromlist=["getSampleStyleSheet"])
                    .getSampleStyleSheet()["Title"]
                ),
                table
            ])
            QMessageBox.information(self, "Berhasil", f"PDF tersimpan:\n{path}")
        except Exception as exc:
            QMessageBox.critical(self, "Gagal ekspor PDF", str(exc))

    def export_excel(self):
        from PySide6.QtWidgets import QFileDialog
        path, _ = QFileDialog.getSaveFileName(
            self, "Simpan Excel", "stok_bahan.xlsx", "Excel (*.xlsx)"
        )
        if not path:
            return
        try:
            title, data = self.export_rows()
            wb = Workbook()
            ws = wb.active
            ws.title = "Stok Bahan"
            ws.page_setup.orientation = "portrait"
            ws.page_setup.paperSize = ws.PAPERSIZE_A4
            ws.page_setup.fitToWidth = 1
            ws.page_setup.fitToHeight = 0
            ws.sheet_properties.pageSetUpPr.fitToPage = True
            ws.merge_cells(start_row=1, start_column=1, end_row=1, end_column=7)
            ws.cell(1, 1, title or "STOK BAHAN")
            ws.cell(1, 1).font = Font(bold=True, size=16, color="FFFFFF")
            ws.cell(1, 1).fill = PatternFill("solid", fgColor="2457D6")
            ws.cell(1, 1).alignment = Alignment(horizontal="center")

            for row_index, row in enumerate(data, start=3):
                for col_index, value in enumerate(row, start=1):
                    cell = ws.cell(row_index, col_index, value)
                    cell.alignment = Alignment(vertical="center")
                    cell.border = Border(
                        bottom=Side(style="thin", color="D8E0EB")
                    )
                    if row_index == 3:
                        cell.font = Font(bold=True, color="FFFFFF")
                        cell.fill = PatternFill("solid", fgColor="2457D6")

            for column in ws.columns:
                letter = column[0].column_letter
                ws.column_dimensions[letter].width = min(
                    max(14, max(len(str(c.value or "")) for c in column) + 2), 32
                )
            ws.freeze_panes = "A4"
            wb.save(path)
            QMessageBox.information(self, "Berhasil", f"Excel tersimpan:\n{path}")
        except Exception as exc:
            QMessageBox.critical(self, "Gagal ekspor Excel", str(exc))

    def closeEvent(self, event):
        self.db.close()
        event.accept()


if __name__ == "__main__":
    app = QApplication(sys.argv)
    window = StokBahanApp()

    icon_path = resource_path("logo_sinar_mas.ico")
    if os.path.exists(icon_path):
        window.setWindowIcon(QIcon(icon_path))
        app.setWindowIcon(QIcon(icon_path))

    window.show()
    sys.exit(app.exec())
