import React from "react";
import clsx from "clsx";
import Link from "@docusaurus/Link";
import useDocusaurusContext from "@docusaurus/useDocusaurusContext";
import Layout from "@theme/Layout";
import HomepageFeatures from "@site/src/components/HomepageFeatures";
import HomepageBenefits from "@site/src/components/HomepageBenefits";

import styles from "./index.module.css";

const HeroSplitSection: React.FC = () => {
  return (
    <section className={styles.splitSection}>
      <div className={styles.techBg}></div>
      <div className="container">
        <div className={styles.splitInner}>
          <div className={styles.splitLeft}>
            <h2 className={styles.splitHeading}>
              Hướng tới chuyển đổi số toàn dân
            </h2>
            <p className={styles.splitText}>
              {/* Tên dự án đã chính xác */}
              Ldx-Insight (Chuyển đổi số Địa phương) — hợp nhất dữ liệu mở, hiển
              thị trực quan và đánh giá bộ chỉ số để hỗ trợ ra quyết định của
              chính quyền địa phương.
            </p>
            <div className={styles.splitButtons}>
              <Link
                className={clsx("button", styles.smallPrimary)}
                to="/overview/intro" // Đường dẫn này rất tốt
              >
                Khám phá ngay
              </Link>
            </div>
            <div className={styles.statsRow}>
              <div className={styles.statItem}>
                <div className={styles.statNumber}>100%</div>
                <div className={styles.statLabel}>Mở và miễn phí</div>
              </div>
              <div className={styles.statItem}>
                {/* 💡 Sửa lại: Tập trung vào REST API như trong kế hoạch */}
                <div className={styles.statNumber}>RESTful API</div>
                <div className={styles.statLabel}>API Mở</div>
              </div>
              <div className={styles.statItem}>
                {/* 💡 Sửa lại: Thay "24/7" bằng một đặc điểm kỹ thuật thực tế */}
                <div className={styles.statNumber}>Microservice</div>
                <div className={styles.statLabel}>Kiến trúc</div>
              </div>
            </div>
          </div>

          <div className={styles.splitRight}>
            <div className={styles.codeCard}>
              <div className={styles.cardHeader}>
                <div className={styles.circles}>
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
                <div className={styles.cardTitle}>Terminal</div>
              </div>
              <div className={styles.cardBody}>
                <div className={styles.promptLine}>
                  <span className={styles.prompt}>$</span>
                  {/* Ví dụ API này rất chuyên nghiệp */}
                  <span className={styles.command}>
                    {" "}
                    curl https://api.ldx-insight.io/data
                  </span>
                </div>
                <pre className={styles.response}>
                  {`               {
                 "status": "success",
                 "data": [{
                   "id": "dataset-001",
                   "name": "Ldx-Insight Data", // Tên đã chính xác
                   "records": 1000000
                 }]
               }`}
                </pre>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};

const Home: React.FC = () => {
  const { siteConfig } = useDocusaurusContext();
  return (
    <Layout
      title={`${siteConfig.title} - Nền tảng dữ liệu mở`}
      // Mô tả đã rất phù hợp
      description="L-DX (Chuyển đổi số Địa phương) — hợp nhất dữ liệu mở, hiển thị trực quan và đánh giá bộ chỉ số để hỗ trợ ra quyết định của chính quyền địa phương."
    >
      <div className={styles.pageWrapper}>
        <main>
          <HeroSplitSection />
          <HomepageFeatures />
          <HomepageBenefits />
        </main>
      </div>
    </Layout>
  );
};

export default Home;
