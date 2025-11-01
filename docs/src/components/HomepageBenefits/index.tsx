import type { ReactNode } from "react";
import clsx from "clsx";
import Heading from "@theme/Heading";
import styles from "./styles.module.css";

type BenefitItem = {
  title: string;
  description: string;
  icon: string;
};

const BenefitsList: BenefitItem[] = [
  {
    title: "Giảm thời gian phát triển",
    description:
      "Sử dụng API chuẩn hóa để tích hợp dữ liệu nhanh chóng mà không cần xây dựng lại từ đầu.",
    icon: "⚙️",
  },
  {
    title: "Tăng chất lượng dữ liệu",
    description:
      "Dữ liệu được chuẩn hóa, xác thực và liên kết, đảm bảo độ chính xác cao.",
    icon: "✓",
  },
  {
    title: "Mở rộng dễ dàng",
    description:
      "Kiến trúc microservices cho phép bạn mở rộng từng thành phần độc lập.",
    icon: "📈",
  },
  {
    title: "Cộng đồng hỗ trợ",
    description:
      "Tham gia cộng đồng mã nguồn mở, nhận hỗ trợ và đóng góp ý tưởng.",
    icon: "👥",
  },
];

function Benefit({ title, description, icon }: BenefitItem) {
  return (
    <div className={clsx("col col--6", styles.benefitCol)}>
      <div className={styles.benefitCard}>
        <div className={styles.benefitIcon}>{icon}</div>
        <Heading as="h3" className={styles.benefitTitle}>
          {title}
        </Heading>
        <p className={styles.benefitDescription}>{description}</p>
      </div>
    </div>
  );
}

export default function HomepageBenefits(): ReactNode {
  return (
    <section className={styles.benefits}>
      <div className="container">
        <div className={styles.benefitsHeader}>
          <Heading as="h2" className={styles.benefitsTitle}>
            Tại sao chọn Ldx-Insight?
          </Heading>
          <p className={styles.benefitsSubtitle}>
            Giải pháp toàn diện cho quản lý và chia sẻ dữ liệu mở, trợ giúp chẩn
            đoán và đề xuất chuyển đổi số
          </p>
        </div>
        <div className="row">
          {BenefitsList.map((props, idx) => (
            <Benefit key={idx} {...props} />
          ))}
        </div>
      </div>
    </section>
  );
}
