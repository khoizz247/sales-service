"""Turn real Locust CSV outputs into a compact, auditable load-test report."""

import argparse
import csv
import json
from pathlib import Path


PHASES = ("smoke", "baseline", "main", "high")


def read_phase(run_dir, phase, config):
    csv_path = run_dir / f"{phase}_stats.csv"
    if not csv_path.is_file():
        return None
    with csv_path.open(encoding="utf-8-sig", newline="") as stream:
        rows = list(csv.DictReader(stream))
    aggregate = next((row for row in rows if row.get("Name", "").lower().startswith("aggregat")), None)
    if aggregate is None:
        raise ValueError(f"No aggregate row in {csv_path}")

    def number(key):
        value = aggregate.get(key)
        if value is None or value == "":
            raise ValueError(f"Missing {key} in {csv_path}")
        return float(value)

    requests = int(number("Request Count"))
    failures = int(number("Failure Count"))
    return {
        "phase": phase,
        "users": config["users"],
        "duration_seconds": config["duration_seconds"],
        "requests": requests,
        "failures": failures,
        "failure_percent": round(100 * failures / requests, 2) if requests else 0,
        "rps": round(number("Requests/s"), 2),
        "p50_ms": round(number("50%"), 2),
        "p95_ms": round(number("95%"), 2),
        "p99_ms": round(number("99%"), 2),
        "exit_code": config.get("exit_code", ""),
    }


def build_report(run_dir):
    metadata = json.loads((run_dir / "metadata.json").read_text(encoding="utf-8"))
    results = [read_phase(run_dir, phase, metadata["phases"][phase])
               for phase in PHASES if phase in metadata["phases"]]
    results = [row for row in results if row is not None]
    if not results:
        raise ValueError("No Locust phase CSV files found")

    fields = list(results[0])
    with (run_dir / "summary.csv").open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=fields)
        writer.writeheader()
        writer.writerows(results)

    lines = [
        "# Báo cáo kiểm thử tải Sales Service",
        "",
        f"- Thời điểm UTC: {metadata['started_at_utc']}",
        f"- Git commit: `{metadata['git_commit']}`",
        f"- Nơi chạy: {metadata['environment']}",
        f"- CPU: {metadata['cpu_count']} luồng; RAM: {metadata['memory_gb']} GiB",
        f"- Java: {metadata['java_version']}",
        f"- MySQL: {metadata['mysql_version']}",
        f"- Locust: {metadata['locust_version']}",
        "- API và MySQL chạy trên cùng phiên CPU; Locust gọi qua localhost.",
        "- Trọng số tác vụ mục tiêu: 60% danh sách sản phẩm, 15% chi tiết, 10% đăng nhập, "
        "10% đơn của tôi, 5% tạo đơn.",
        "- Mỗi người dùng ảo có một sản phẩm tồn kho lớn riêng; request chuẩn bị "
        "tài khoản/sản phẩm không tính vào số liệu Locust.",
        "",
        "| Pha | Users | Giây | Requests | Lỗi | Lỗi % | RPS | p50 ms | p95 ms | p99 ms |",
        "|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|",
    ]
    for row in results:
        lines.append("| {phase} | {users} | {duration_seconds} | {requests} | {failures} | "
                     "{failure_percent} | {rps} | {p50_ms} | {p95_ms} | {p99_ms} |".format(**row))
    if metadata.get("high_skipped_reason"):
        lines += ["", f"Pha tải cao đã bỏ qua: {metadata['high_skipped_reason']}."]
    lines += [
        "",
        "## Nhận xét cần hoàn thiện sau khi xem log",
        "",
        "- Kiểm tra `*_failures.csv` và `api.log` trước khi kết luận không có lỗi 5xx.",
        "- So sánh p95, p99 và RPS khi số người dùng tăng; ghi rõ điểm bắt đầu suy giảm.",
        "- Các pha chạy tuần tự trên cùng database, dữ liệu người dùng/đơn tăng dần; "
        "đây là giới hạn của phép so sánh giữa các pha.",
        "- Kết quả là baseline Pha 1, không phải cam kết hiệu năng production.",
        "",
        "## Tệp minh chứng",
        "",
        "`metadata.json`, `summary.csv`, `*_stats.csv`, `*_failures.csv`, "
        "`*_stats_history.csv`, `*_report.html`, `api.log`.",
        "",
    ]
    (run_dir / "REPORT.md").write_text("\n".join(lines), encoding="utf-8")
    return results


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("run_dir", type=Path)
    args = parser.parse_args()
    for result in build_report(args.run_dir):
        print(result)
