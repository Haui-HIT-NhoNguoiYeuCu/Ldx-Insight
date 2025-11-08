export type SummaryStats = {
  totalDatasets: number;
  totalViews: number;
  totalDownloads: number;
};

export type CategoryStat = {
  category: string | null;
  count: number;
};
