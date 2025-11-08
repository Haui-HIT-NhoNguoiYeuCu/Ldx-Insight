export type Dataset = {
  id: string;
  title: string;
  description: string;
  source: string;
  tags: string[];
  category: string;
  viewCount: number;
  downloadCount: number;
  provider: string;
  createdAt: string;
  updatedAt: string;
};

export type DatasetRequestParams = {
  q?: string;
  category?: string;
  page?: number;
  size?: number;
  sort?: string;
};

export type DatasetResponse = {
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
  content: any[];
  first: boolean;
  last: boolean;
};
